package prowse.github.cosm1c.http.importer

import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import io.circe.Json.fromJsonObject
import io.circe._
import io.circe.generic.semiauto._
import io.circe.java8.time.TimeInstances
import prowse.github.cosm1c.http.importer.job.JobManagerActor.JobInfo

trait JsonProtocol extends FailFastCirceSupport with TimeInstances {

    implicit val circePrinter: Printer = Printer.noSpaces //.copy(dropNullValues = true)


    implicit val jobInfoDecoder: Decoder[JobInfo] = deriveDecoder[JobInfo]

    implicit val jobInfoEncoder: Encoder[JobInfo] =
        deriveEncoder[JobInfo]
            // This could be recursive
            .mapJsonObject(_.filter(!_._2.isNull))


    def conflateJsonKeepNulls(state: Json, delta: Json): Json =
        state.deepMerge(delta)

    def conflateJsonDropNulls(state: Json, delta: Json): Json =
        (state.asObject, delta.asObject) match {
            case (Some(lhs), Some(rhs)) =>
                fromJsonObject(
                    lhs.toList.foldLeft(rhs) {
                        case (acc, (key, value)) =>
                            rhs(key).fold(acc.add(key, value)) { r =>
                                if (r.isNull) acc.remove(key)
                                else acc.add(key, conflateJsonDropNulls(value, r))
                            }
                    }
                )
            case _ => delta
        }

    def applyJsonDelta(state: (Json, Json), delta: Json): (Json, Json) =
        (conflateJsonDropNulls(state._1, delta), delta)

    def conflateJsonPair(state: (Json, Json), delta: (Json, Json)): (Json, Json) =
        (conflateJsonDropNulls(state._1, delta._2), conflateJsonKeepNulls(state._2, delta._2))

}
