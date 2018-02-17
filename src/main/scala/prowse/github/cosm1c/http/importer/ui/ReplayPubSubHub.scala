package prowse.github.cosm1c.http.importer.ui

import akka.NotUsed
import akka.stream.Materializer
import akka.stream.scaladsl.{MergeHub, Sink, Source}
import io.circe.Json
import org.reactivestreams.Processor
import prowse.github.cosm1c.http.importer.JsonProtocol
import prowse.github.cosm1c.http.importer.ui.UiWebSocketFlow.emptyJson
import reactor.core.publisher.ReplayProcessor

object ReplayPubSubHub {

    private final val emptyJsonTuple = (emptyJson, emptyJson)
}

abstract class ReplayPubSubHub()(implicit materializer: Materializer) extends JsonProtocol {

    import ReplayPubSubHub._

    private val processor: Processor[(Json, Json), (Json, Json)] =
        ReplayProcessor.cacheLast[(Json, Json)]

    protected val inSink: Sink[Json, NotUsed] =
        MergeHub.source[Json](perProducerBufferSize = 1)
            .scan(emptyJsonTuple)(applyJsonDelta)
            .conflate(conflateJsonPair)
            .to(Sink.fromSubscriber(processor))
            .run()

    protected val outSource: Source[(Json, Json), NotUsed] =
        Source.fromPublisher(processor)

}
