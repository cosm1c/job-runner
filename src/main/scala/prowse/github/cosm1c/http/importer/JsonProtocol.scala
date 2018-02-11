package prowse.github.cosm1c.http.importer

import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import io.circe.Json.fromJsonObject
import io.circe.generic.semiauto._
import io.circe.java8.time.TimeInstances
import io.circe.{KeyEncoder, _}
import prowse.github.cosm1c.http.importer.job.JobManagerActor.JobInfo

/*
 * Json maps are trimmed of nulls which make conflation a simple deepMerge.
 */
trait JsonProtocol extends FailFastCirceSupport with TimeInstances {

    implicit val circePrinter: Printer = Printer.noSpaces.copy(dropNullValues = true)

    implicit val jobInfoKeyEncoder: KeyEncoder[JobInfo] = _.jobId.toString

    implicit val jobInfoDecoder: Decoder[JobInfo] = deriveDecoder[JobInfo]

    implicit val jobInfoEncoder: Encoder[JobInfo] = deriveEncoder[JobInfo]

    def conflateJson(curr: Json, update: Json): Json =
        (curr.asObject, update.asObject) match {
            case (Some(lhs), Some(rhs)) =>
                fromJsonObject(
                    lhs.toList.foldLeft(rhs.filter(!_._2.isNull)) {
                        case (acc, (key, value)) =>
                            rhs(key).fold(acc.add(key, value)) { r => acc.add(key, conflateJson(value, r)) }
                    }
                )

            case _ =>
                if (update.isNull) curr
                else update
        }

}
