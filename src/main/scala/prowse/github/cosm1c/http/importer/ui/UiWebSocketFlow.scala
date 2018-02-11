package prowse.github.cosm1c.http.importer.ui

import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.stream.scaladsl.{BroadcastHub, Flow, Keep, MergeHub, Sink, Source}
import akka.stream.{Materializer, ThrottleMode}
import akka.{Done, NotUsed}
import io.circe._
import io.circe.syntax._
import prowse.github.cosm1c.http.importer.JsonProtocol

import scala.concurrent.Future
import scala.concurrent.duration._

object UiWebSocketFlow {

    private final val keepAliveWebSocketFrame = () => TextMessage.Strict("{}")
}

class UiWebSocketFlow()(implicit materializer: Materializer) extends JsonProtocol {

    import UiWebSocketFlow._

    private val (broadcastSink, broadcastSource): (Sink[Json, NotUsed], Source[Json, NotUsed]) =
        MergeHub.source[Json](perProducerBufferSize = 1)
            .conflate(conflateJson)
            .toMat(BroadcastHub.sink(bufferSize = 1))(Keep.both)
            .run()

    broadcastSource.runWith(Sink.ignore)

    val clientWebSocketFlow: Flow[Message, Message, Any] = {

        val in: Sink[Any, Future[Done]] = Sink.ignore

        // TODO: use RxJava Subject to provide 1st message, 1st message accumulated from scan(emptyJson)(conflateJson)
        val out: Source[Message, NotUsed] =
            broadcastSource
                .conflate(conflateJson)
                // Throttle to avoid overloading frontend - increase duration for potentially lower bandwidth
                .throttle(1, 100.millis, 1, ThrottleMode.Shaping)
                .map(circePrinter.pretty)
                .map(TextMessage.Strict)
                .keepAlive(55.seconds, keepAliveWebSocketFrame)

        Flow.fromSinkAndSourceCoupledMat(in, out)(Keep.both)
    }

    def attachSubSource[A, M](subSource: Source[A, M])(implicit keyEncoder: KeyEncoder[A], encoder: Encoder[A]): M =
        subSource
            .map(msg => Map(keyEncoder(msg) -> msg).asJson)
            .toMat(broadcastSink)(Keep.left)
            .run()

}
