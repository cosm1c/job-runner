package prowse.github.cosm1c.http.importer.ui

import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.stream.scaladsl.{Flow, Keep, MergeHub, Sink, Source}
import akka.stream.{Materializer, ThrottleMode}
import akka.{Done, NotUsed}
import io.circe._
import io.circe.syntax._
import org.reactivestreams.Processor
import prowse.github.cosm1c.http.importer.JsonProtocol
import reactor.core.publisher.ReplayProcessor

import scala.concurrent.Future
import scala.concurrent.duration._

object UiWebSocketFlow {

    private final val emptyJson = Json.obj()

    private final val emptyJsonTuple = (emptyJson, emptyJson)

    private final val keepAliveWebSocketFrame = () => TextMessage.Strict("{}")
}

class UiWebSocketFlow()(implicit materializer: Materializer) extends JsonProtocol {

    import UiWebSocketFlow._

    private val processor: Processor[(Json, Json), (Json, Json)] =
        ReplayProcessor.cacheLast[(Json, Json)]

    private val inSink: Sink[Json, NotUsed] =
        MergeHub.source[Json](perProducerBufferSize = 1)
            .scan(emptyJsonTuple)(applyJsonDelta)
            .conflate(conflateJsonPair)
            .to(Sink.fromSubscriber(processor))
            .run()

    private val outSource: Source[(Json, Json), NotUsed] =
        Source.fromPublisher(processor)

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

    def attachSubSource[A, M](streamId: String, subSource: Source[A, M])(implicit encoder: Encoder[A]): M =
        subSource
            .map(msg => Map(streamId -> msg).asJson)
            .concat(Source.single(Json.obj(streamId -> Json.Null))) // remove from state
            .recover { case _ => Json.obj(streamId -> Json.Null) }
            .toMat(inSink)(Keep.left)
            .run()

}
