package prowse.github.cosm1c.http.importer.ui

import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import akka.stream.{Materializer, ThrottleMode}
import akka.{Done, NotUsed}
import io.circe._
import io.circe.syntax._

import scala.concurrent.Future
import scala.concurrent.duration._

object UiWebSocketFlow {

    final val emptyJson = Json.obj()

    private final val keepAliveWebSocketFrame = () => TextMessage.Strict("{}")
}

class UiWebSocketFlow()(implicit materializer: Materializer) extends ReplayPubSubHub {

    import UiWebSocketFlow._

    val clientWebSocketFlow: Flow[Message, Message, Any] = {

        val in: Sink[Any, Future[Done]] = Sink.ignore

        val out: Source[Message, NotUsed] =
            outSource
                .conflate(conflateJsonPair)
                // Throttle to avoid overloading frontend - increase duration for potentially lower bandwidth
                .throttle(1, 100.millis, 1, ThrottleMode.Shaping)
                .prefixAndTail(1)
                .flatMapConcat { case (head, tail) =>
                    Source.single(
                        head.headOption
                            .map(_._1)
                            .getOrElse(emptyJson)
                    ).concat(tail.map(_._2))
                }
                .map(circePrinter.pretty)
                .map(TextMessage.Strict)
                .keepAlive(55.seconds, keepAliveWebSocketFrame)

        Flow.fromSinkAndSourceCoupledMat(in, out)(Keep.both)
    }

    def attachSubSource[A, M](subSource: Source[A, M])(implicit keyEncoder: KeyEncoder[A], encoder: Encoder[A]): M =
        subSource
            .map(msg => Map(keyEncoder(msg) -> msg).asJson)
            .toMat(inSink)(Keep.left)
            .run()

}
