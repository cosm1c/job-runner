package prowse.github.cosm1c.http.importer.job

import java.time.LocalDateTime

import akka.actor.{Actor, ActorLogging, Props}
import akka.stream._
import akka.stream.scaladsl.{Keep, MergeHub, Sink, Source}
import akka.{Done, NotUsed}
import prowse.github.cosm1c.http.importer.JsonProtocol
import prowse.github.cosm1c.http.importer.job.JobManagerActor._
import prowse.github.cosm1c.http.importer.ui.UiWebSocketFlow
import prowse.github.cosm1c.http.importer.util.ReplyStatus
import prowse.github.cosm1c.http.importer.util.ReplyStatus.{ReplyFailure, ReplySuccess}

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}

object JobManagerActor {

    def props(uiStreams: UiWebSocketFlow)(implicit materializer: Materializer): Props =
        Props(new JobManagerActor(uiStreams))

    final case class JobInfo(jobId: Long,
                             curr: Option[Int] = None,
                             total: Option[Int] = None,
                             startDateTime: Option[LocalDateTime] = None,
                             endDateTime: Option[LocalDateTime] = None,
                             error: Option[String] = None,
                             description: Option[String] = None)

    final case object ListRunningJobs

    final case class GetJobInfo(jobId: Long)

    final case class KillJob(jobId: Long)

    final case class CreateJob(description: String,
                               total: Int)


    private final case class JobCompleted(jobId: Long,
                                          endDateTime: LocalDateTime,
                                          error: Option[String] = None)


    private class JobKilledException extends Exception("Job Killed")

    private case object JobInfoStreamStart

    private case object JobInfoStreamAck

    private case object JobInfoStreamEnd

}

@SuppressWarnings(Array("org.wartremover.warts.Var"))
class JobManagerActor(uiStreams: UiWebSocketFlow)(implicit materializer: Materializer) extends Actor with JsonProtocol with ActorLogging {

    private var jobCounter: Long = 0L
    private var jobInfos = Map.empty[Long, JobInfo]
    private var jobKillSwitches = Map.empty[Long, UniqueKillSwitch]

    private val jobInfoStreamMergeHubSource: Sink[JobInfo, NotUsed] =
        MergeHub.source[JobInfo](perProducerBufferSize = 1)
            .toMat(Sink.actorRefWithAck(self, JobInfoStreamStart, JobInfoStreamAck, JobInfoStreamEnd, throwable => log.error(throwable, "JobInfo Stream failed")))(Keep.left)
            .run()

    override def receive: Receive = {

        case jobInfo: JobInfo =>
            jobInfos += jobInfo.jobId -> jobInfo
            sender ! JobInfoStreamAck

        case JobInfoStreamStart => sender ! JobInfoStreamAck

        case ListRunningJobs => sender() ! jobInfos.values

        case GetJobInfo(jobId) => sender() ! jobInfos.get(jobId)

        case KillJob(jobId) =>
            val reply: ReplyStatus.Reply = jobKillSwitches.get(jobId) match {
                case Some(killSwitch) =>
                    log.info("[{}] Killing job", jobId)
                    killSwitch.abort(new JobKilledException)
                    ReplySuccess

                case None => ReplyFailure
            }
            sender() ! reply

        case CreateJob(description, total) =>
            val jobId = nextJobId
            val zeroJobInfo = JobInfo(jobId, Some(0), Some(total), Some(LocalDateTime.now()), description = Some(description))

            val wrappedJobStream: Source[JobInfo, (UniqueKillSwitch, Future[Done])] =
                jobInfoStream(jobId, zeroJobInfo, total)
                    .alsoTo(jobInfoStreamMergeHubSource)
                    .viaMat(KillSwitches.single)(Keep.right)
                    .alsoToMat(Sink.ignore)(Keep.both)
                    .mapMaterializedValue(m => {
                        log.info("[{}] Started", jobId)
                        m
                    })
                    .recover {
                        case throwable: Throwable =>
                            JobInfo(jobId, endDateTime = Some(LocalDateTime.now()), error = Some(throwable.getMessage))
                    }
                    .concat(Source.lazily(() => {
                        Source.single(
                            JobInfo(jobId, endDateTime = Some(LocalDateTime.now())))
                    }))

            val (killSwitch, eventualDone) = uiStreams.attachSubSource(wrappedJobStream)

            eventualDone.onComplete {
                case Success(_) =>
                    log.info("[{}] complete", jobId)
                    self ! JobCompleted(jobId, LocalDateTime.now)

                case Failure(ex) =>
                    ex match {
                        case e: JobKilledException => log.warning("[{}] {}", jobId, e.getMessage)
                        case e => log.error(e, "[{}] error: {}", jobId, e.getMessage)
                    }
                    self ! JobCompleted(jobId, LocalDateTime.now, error = Some(ex.getMessage))
            }(context.dispatcher)

            jobInfos += jobId -> zeroJobInfo
            jobKillSwitches += jobId -> killSwitch
            sender() ! zeroJobInfo

        case JobCompleted(jobId, _, _) =>
            // Example only - could persist job result
            jobInfos -= jobId
            jobKillSwitches -= jobId
    }

    private def jobInfoStream(jobId: Long, zeroJobInfo: JobInfo, total: Int): Source[JobInfo, NotUsed] =
        if (total < 0)
            Source.failed(new RuntimeException("Total < 0"))
        else
            Source.single(zeroJobInfo)
                .concat(
                    Source(1 to total)
                        .throttle(1, 10.milliseconds, 1, ThrottleMode.shaping)
                        .map(count => JobInfo(jobId, Some(count)))
                )

    private def nextJobId = {
        jobCounter += 1
        jobCounter
    }

}
