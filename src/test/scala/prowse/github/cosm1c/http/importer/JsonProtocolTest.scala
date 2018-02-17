package prowse.github.cosm1c.http.importer

import io.circe.Json
import org.scalatest._

class JsonProtocolTest extends FlatSpec {

    private val protocol = new JsonProtocol {}

    "testConflateJson" should "overwrite non JSObjects" in {
        assertOverwrites(Json.fromInt(1), Json.fromInt(2))
    }

    it should "recurse one deep" in {
        assertOverwrites(
            Json.fromFields(Seq(
                "a" -> Json.fromInt(1)
            )),
            Json.fromFields(Seq(
                "a" -> Json.fromInt(2)
            ))
        )
    }

    it should "recurse two deep" in {
        assertOverwrites(
            Json.fromFields(Seq(
                "parent" -> Json.fromFields(Seq(
                    "a" -> Json.fromInt(1)
                ))
            )),
            Json.fromFields(Seq(
                "parent" -> Json.fromFields(Seq(
                    "a" -> Json.fromInt(2)
                ))
            ))
        )
    }

    it should "recurse merge trees" in {
        assert(
            protocol.conflateJson(
                Json.fromFields(Seq(
                    "parent" -> Json.fromFields(Seq(
                        "a" -> Json.fromInt(1)
                    ))
                )),
                Json.fromFields(Seq(
                    "b" -> Json.fromInt(2)
                ))
            ) == Json.fromFields(Seq(
                "parent" -> Json.fromFields(Seq(
                    "a" -> Json.fromInt(1)
                )),
                "b" -> Json.fromInt(2)
            ))
        )
    }

    it should "recurse merge sub trees" in {
        assert(
            protocol.conflateJson(
                Json.fromFields(Seq(
                    "parent" -> Json.fromFields(Seq(
                        "a" -> Json.fromInt(1)
                    ))
                )),
                Json.fromFields(Seq(
                    "parent" -> Json.fromFields(Seq(
                        "b" -> Json.fromInt(2)
                    ))
                ))
            ) == Json.fromFields(Seq(
                "parent" -> Json.fromFields(Seq(
                    "a" -> Json.fromInt(1),
                    "b" -> Json.fromInt(2)
                ))
            ))
        )
    }

    it should "recurse merge sub trees ignoring nulls" in {
        assert(
            protocol.conflateJson(
                Json.fromFields(Seq(
                    "parent" -> Json.fromFields(Seq(
                        "a" -> Json.fromInt(1),
                        "b" -> Json.Null
                    ))
                )),
                Json.fromFields(Seq(
                    "parent" -> Json.fromFields(Seq(
                        "a" -> Json.Null,
                        "b" -> Json.fromInt(2)
                    ))
                ))
            ) == Json.fromFields(Seq(
                "parent" -> Json.fromFields(Seq(
                    "a" -> Json.fromInt(1),
                    "b" -> Json.fromInt(2)
                ))
            ))
        )
    }

    it should "ignore nulls" in {
        assertPreserves(Json.fromInt(1), Json.Null)
    }

    it should "ignore nulls in JSObjects" in {
        assertPreserves(
            Json.fromFields(Seq(
                "b" -> Json.fromInt(2)
            )),
            Json.fromFields(Seq(
                "b" -> Json.Null
            ))
        )
    }

    it should "ignore nulls in nested JSObjects" in {
        assertPreserves(
            Json.fromFields(Seq(
                "parent" -> Json.fromFields(Seq(
                    "b" -> Json.fromInt(2)
                ))
            )),
            Json.fromFields(Seq(
                "parent" -> Json.fromFields(Seq(
                    "b" -> Json.Null
                ))
            ))
        )
    }


    private def assertOverwrites(self: Json, that: Json) = assert(protocol.conflateJson(self, that) == that)

    private def assertPreserves(self: Json, that: Json) = assert(protocol.conflateJson(self, that) == self)

}
