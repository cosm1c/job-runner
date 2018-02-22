package prowse.github.cosm1c.http.importer.ui

import akka.event.LoggingAdapter
import akka.http.scaladsl.server._
import akka.stream.OverflowStrategy
import akka.stream.QueueOfferResult.{Dropped, Enqueued, QueueClosed, _}
import akka.stream.scaladsl.{Source, SourceQueueWithComplete}

import scala.concurrent.ExecutionContext

class UiRoutes(uiWebSocketFlow: UiWebSocketFlow)(implicit executor: ExecutionContext, log: LoggingAdapter) extends Directives {

    private val userCountSource: Source[Int, SourceQueueWithComplete[Int]] =
        Source
            .queue(0, OverflowStrategy.backpressure)
            .scan(0)(_ + _)

    private val userCountSourceQueue: SourceQueueWithComplete[Int] =
        uiWebSocketFlow.attachSubSource("userCount", userCountSource)

    val route: Route =
        pathEndOrSingleSlash {
            getFromResource("ui/index.html")
        } ~ path("ws") {
            onSuccess(userCountSourceQueue.offer(1)) {
                case Enqueued =>
                    handleWebSocketMessages(
                        uiWebSocketFlow.clientWebSocketFlow
                            .watchTermination()((_, done) => done.foreach(_ => userCountSourceQueue.offer(-1))))

                case Failure(cause) =>
                    log.error(cause, "Failed to enqueue websocket message - Failure")
                    failWith(cause)

                case QueueClosed =>
                    log.error("Failed to enqueue websocket message - QueueClosed")
                    failWith(new RuntimeException("Failed to enqueue websocket message - QueueClosed"))

                case Dropped =>
                    log.error("Packet dropped instead of enqueued - Dropped")
                    failWith(new RuntimeException("Packet dropped instead of enqueued - Dropped"))
            }
        } ~ getFromResourceDirectory("ui")

}
