package prowse.github.cosm1c.http.importer.job

import javax.ws.rs.Path

import akka.actor.ActorRef
import akka.http.scaladsl.model.StatusCodes.{NotFound, OK}
import akka.http.scaladsl.server._
import akka.pattern.ask
import akka.util.Timeout
import io.swagger.annotations._
import prowse.github.cosm1c.http.importer.JsonProtocol
import prowse.github.cosm1c.http.importer.job.JobManagerActor._
import prowse.github.cosm1c.http.importer.util.ReplyStatus.{Reply, ReplyFailure, ReplySuccess}

import scala.concurrent.duration._

@Api(produces = "application/json", tags = Array("jobs"))
@Path("job")
class JobRestService(jobManagerActor: ActorRef) extends Directives with JsonProtocol {

    private implicit val timeout: Timeout = Timeout(1.second)

    val route: Route =
        pathPrefix("job") {
            listRunningJobs ~
                createJob ~
                getJobInfo ~
                killJob
        }

    @ApiOperation(value = "List Jobs", notes = "List all jobs currently running", httpMethod = "GET", response = classOf[JobInfo], responseContainer = "List")
    @Path("/")
    def listRunningJobs: Route =
        get {
            pathEndOrSingleSlash {
                onSuccess((jobManagerActor ? ListRunningJobs).mapTo[Iterable[JobInfo]]) { jobInfoIterable =>
                    complete(jobInfoIterable)
                }
            }
        }

    @ApiOperation(value = "Create Job", notes = "Creates demo job which counts up to total", httpMethod = "POST", response = classOf[JobInfo])
    @ApiImplicitParams(Array(
        new ApiImplicitParam(name = "description", value = "Description of Job", required = true, dataTypeClass = classOf[String], paramType = "query"),
        new ApiImplicitParam(name = "total", value = "Total items to process", required = true, dataTypeClass = classOf[Int], paramType = "query")
    ))
    @Path("/")
    def createJob: Route =
        post {
            pathEndOrSingleSlash {
                parameters(('description, 'total.as[Int])) { (description, total) =>
                    onSuccess((jobManagerActor ? CreateJob(description, total)).mapTo[JobInfo]) { jobInfo =>
                        complete(jobInfo)
                    }
                }
            }
        }

    @ApiOperation(value = "Fetch JobInfo", notes = "Fetch current state of job", httpMethod = "GET", response = classOf[JobInfo])
    @ApiImplicitParams(Array(
        new ApiImplicitParam(name = "jobId", value = "Job ID", required = true, dataTypeClass = classOf[Long], paramType = "path")
    ))
    @ApiResponses(Array(
        new ApiResponse(code = 200, message = "Job exists"),
        new ApiResponse(code = 404, message = "Job does not exist")
    ))
    @Path("{jobId}")
    def getJobInfo: Route =
        get {
            path(LongNumber) { jobId =>
                onSuccess((jobManagerActor ? GetJobInfo(jobId)).mapTo[Option[JobInfo]]) {

                    case Some(jobInfo) => complete(jobInfo)

                    case None => complete(NotFound)
                }
            }
        }

    @ApiOperation(value = "Kill Job", notes = "Kill a running Job", httpMethod = "DELETE")
    @ApiImplicitParams(Array(
        new ApiImplicitParam(name = "jobId", value = "Job ID", required = true, dataTypeClass = classOf[Long], paramType = "path")
    ))
    @ApiResponses(Array(
        new ApiResponse(code = 200, message = "Job sent kill message"),
        new ApiResponse(code = 404, message = "No such job exists")
    ))
    @Path("{jobId}")
    def killJob: Route =
        delete {
            path(LongNumber) { jobId =>
                onSuccess((jobManagerActor ? KillJob(jobId)).mapTo[Reply]) {

                    case ReplySuccess => complete(OK)

                    case ReplyFailure => complete(NotFound)
                }
            }
        }

}
