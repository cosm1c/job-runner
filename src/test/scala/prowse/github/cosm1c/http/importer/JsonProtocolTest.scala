package prowse.github.cosm1c.http.importer

import io.circe.Json
import org.scalatest._

class JsonProtocolTest extends FunSpec {

    private val protocol = new JsonProtocol {}
    private val EMPTY_JS_OBJECT = Json.fromFields(Seq())
    private val FULL_JS_OBJECT = Json.fromFields(Seq(
        "a" -> Json.fromFields(Seq(
            "b" -> Json.fromFields(Seq(
                "c" -> Json.fromInt(1)
            ))
        )),
        "d" -> Json.fromFields(Seq(
            "e" -> Json.fromInt(2)
        )),
        "f" -> Json.fromInt(3)
    ))

    describe("conflateJsonKeepNulls") {

        it("should handle empty") {
            assert(
                protocol.conflateJsonKeepNulls(
                    EMPTY_JS_OBJECT,
                    FULL_JS_OBJECT
                ) == FULL_JS_OBJECT
            )
            assert(
                protocol.conflateJsonKeepNulls(
                    FULL_JS_OBJECT,
                    EMPTY_JS_OBJECT
                ) == FULL_JS_OBJECT
            )
        }

        it("should handle nulls at root") {
            assert(
                protocol.conflateJsonKeepNulls(
                    FULL_JS_OBJECT,
                    Json.fromFields(Seq(
                        "f" -> Json.Null
                    ))
                ) == Json.fromFields(Seq(
                    "a" -> Json.fromFields(Seq(
                        "b" -> Json.fromFields(Seq(
                            "c" -> Json.fromInt(1)
                        ))
                    )),
                    "d" -> Json.fromFields(Seq(
                        "e" -> Json.fromInt(2)
                    )),
                    "f" -> Json.Null
                ))
            )
            assert(
                protocol.conflateJsonKeepNulls(
                    Json.fromFields(Seq(
                        "f" -> Json.Null
                    )),
                    FULL_JS_OBJECT
                ) == FULL_JS_OBJECT
            )
        }

        it("should handle nulls at depth 2") {
            assert(
                protocol.conflateJsonKeepNulls(
                    FULL_JS_OBJECT,
                    Json.fromFields(Seq(
                        "d" -> Json.fromFields(Seq(
                            "e" -> Json.Null
                        ))
                    ))
                ) == Json.fromFields(Seq(
                    "a" -> Json.fromFields(Seq(
                        "b" -> Json.fromFields(Seq(
                            "c" -> Json.fromInt(1)
                        ))
                    )),
                    "d" -> Json.fromFields(Seq(
                        "e" -> Json.Null
                    )),
                    "f" -> Json.fromInt(3)
                ))
            )
            assert(
                protocol.conflateJsonKeepNulls(
                    Json.fromFields(Seq(
                        "d" -> Json.fromFields(Seq(
                            "e" -> Json.Null
                        ))
                    )),
                    FULL_JS_OBJECT
                ) == FULL_JS_OBJECT
            )
        }

        it("should handle nulls at depth 3") {
            assert(
                protocol.conflateJsonKeepNulls(
                    FULL_JS_OBJECT,
                    Json.fromFields(Seq(
                        "a" -> Json.fromFields(Seq(
                            "b" -> Json.fromFields(Seq(
                                "c" -> Json.Null
                            ))
                        ))
                    ))
                ) == Json.fromFields(Seq(
                    "a" -> Json.fromFields(Seq(
                        "b" -> Json.fromFields(Seq(
                            "c" -> Json.Null
                        ))
                    )),
                    "d" -> Json.fromFields(Seq(
                        "e" -> Json.fromInt(2)
                    )),
                    "f" -> Json.fromInt(3)
                ))
            )
            assert(
                protocol.conflateJsonKeepNulls(
                    Json.fromFields(Seq(
                        "a" -> Json.fromFields(Seq(
                            "b" -> Json.fromFields(Seq(
                                "c" -> Json.Null
                            ))
                        ))
                    )),
                    FULL_JS_OBJECT
                ) == FULL_JS_OBJECT
            )
        }
    }

    describe("conflateJsonDropNulls") {

        it("should handle empty") {
            assert(
                protocol.conflateJsonDropNulls(
                    EMPTY_JS_OBJECT,
                    FULL_JS_OBJECT
                ) == FULL_JS_OBJECT
            )
            assert(
                protocol.conflateJsonDropNulls(
                    FULL_JS_OBJECT,
                    EMPTY_JS_OBJECT
                ) == FULL_JS_OBJECT
            )
        }

        it("should handle nulls at root") {
            assert(
                protocol.conflateJsonDropNulls(
                    FULL_JS_OBJECT,
                    Json.fromFields(Seq(
                        "f" -> Json.Null
                    ))
                ) == Json.fromFields(Seq(
                    "a" -> Json.fromFields(Seq(
                        "b" -> Json.fromFields(Seq(
                            "c" -> Json.fromInt(1)
                        ))
                    )),
                    "d" -> Json.fromFields(Seq(
                        "e" -> Json.fromInt(2)
                    ))
                ))
            )
            assert(
                protocol.conflateJsonDropNulls(
                    Json.fromFields(Seq(
                        "f" -> Json.Null
                    )),
                    FULL_JS_OBJECT
                ) == FULL_JS_OBJECT
            )
        }

        it("should handle nulls at depth 2") {
            assert(
                protocol.conflateJsonDropNulls(
                    FULL_JS_OBJECT,
                    Json.fromFields(Seq(
                        "d" -> Json.fromFields(Seq(
                            "e" -> Json.Null
                        ))
                    ))
                ) == Json.fromFields(Seq(
                    "a" -> Json.fromFields(Seq(
                        "b" -> Json.fromFields(Seq(
                            "c" -> Json.fromInt(1)
                        ))
                    )),
                    "d" -> Json.fromFields(Seq()),
                    "f" -> Json.fromInt(3)
                ))
            )
            assert(
                protocol.conflateJsonDropNulls(
                    Json.fromFields(Seq(
                        "d" -> Json.fromFields(Seq(
                            "e" -> Json.Null
                        ))
                    )),
                    FULL_JS_OBJECT
                ) == FULL_JS_OBJECT
            )
        }

        it("should handle nulls at depth 3") {
            assert(
                protocol.conflateJsonDropNulls(
                    FULL_JS_OBJECT,
                    Json.fromFields(Seq(
                        "a" -> Json.fromFields(Seq(
                            "b" -> Json.fromFields(Seq(
                                "c" -> Json.Null
                            ))
                        ))
                    ))
                ) == Json.fromFields(Seq(
                    "a" -> Json.fromFields(Seq(
                        "b" -> Json.fromFields(Seq())
                    )),
                    "d" -> Json.fromFields(Seq(
                        "e" -> Json.fromInt(2)
                    )),
                    "f" -> Json.fromInt(3)
                ))
            )
            assert(
                protocol.conflateJsonDropNulls(
                    Json.fromFields(Seq(
                        "a" -> Json.fromFields(Seq(
                            "b" -> Json.fromFields(Seq(
                                "c" -> Json.Null
                            ))
                        ))
                    )),
                    FULL_JS_OBJECT
                ) == FULL_JS_OBJECT
            )
        }
    }

    describe("applyJsonDelta") {

        it("should drop null'ed fields") {
            assert(
                protocol.applyJsonDelta(
                    (FULL_JS_OBJECT, FULL_JS_OBJECT),
                    Json.fromFields(Seq(
                        "a" -> Json.fromFields(Seq(
                            "b" -> Json.fromFields(Seq(
                                "c" -> Json.Null
                            ))
                        ))
                    ))
                ) == Tuple2(
                    Json.fromFields(Seq(
                        "a" -> Json.fromFields(Seq(
                            "b" -> Json.fromFields(Seq())
                        )),
                        "d" -> Json.fromFields(Seq(
                            "e" -> Json.fromInt(2)
                        )),
                        "f" -> Json.fromInt(3)
                    )),
                    Json.fromFields(Seq(
                        "a" -> Json.fromFields(Seq(
                            "b" -> Json.fromFields(Seq(
                                "c" -> Json.Null
                            ))
                        ))
                    ))
                )
            )
        }

        it("should overwrite non-Object fields") {
            assert(
                protocol.applyJsonDelta(
                    (EMPTY_JS_OBJECT, EMPTY_JS_OBJECT),
                    FULL_JS_OBJECT
                ) == Tuple2(
                    FULL_JS_OBJECT,
                    FULL_JS_OBJECT
                )
            )
        }

    }

    describe("conflateJsonPair") {

        it("should keep nulls in delta") {
            assert(
                protocol.conflateJsonPair(
                    (FULL_JS_OBJECT, FULL_JS_OBJECT),
                    (
                        Json.fromFields(Seq(
                            "a" -> Json.fromFields(Seq(
                                "b" -> Json.fromFields(Seq(
                                    "c" -> Json.Null
                                ))
                            ))
                        )),
                        Json.fromFields(Seq(
                            "a" -> Json.fromFields(Seq(
                                "b" -> Json.fromFields(Seq(
                                    "c" -> Json.Null
                                ))
                            ))
                        ))
                    )
                ) == Tuple2(
                    Json.fromFields(Seq(
                        "a" -> Json.fromFields(Seq(
                            "b" -> Json.fromFields(Seq())
                        )),
                        "d" -> Json.fromFields(Seq(
                            "e" -> Json.fromInt(2)
                        )),
                        "f" -> Json.fromInt(3)
                    )),
                    Json.fromFields(Seq(
                        "a" -> Json.fromFields(Seq(
                            "b" -> Json.fromFields(Seq(
                                "c" -> Json.Null
                            ))
                        )),
                        "d" -> Json.fromFields(Seq(
                            "e" -> Json.fromInt(2)
                        )),
                        "f" -> Json.fromInt(3)
                    ))
                )
            )
        }
    }

}
