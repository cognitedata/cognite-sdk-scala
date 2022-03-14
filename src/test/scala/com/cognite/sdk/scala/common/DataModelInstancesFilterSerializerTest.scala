// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.common

import com.cognite.sdk.scala.v1.{
  DMIAndFilter,
  DMIContainsAllFilter,
  DMIContainsAnyFilter,
  DMIEqualsFilter,
  DMIExistsFilter,
  DMIInFilter,
  DMINotFilter,
  DMIOrFilter,
  DMIPrefixFilter,
  DMIRangeFilter
}
import io.circe.Json
import io.circe.syntax._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

@SuppressWarnings(
  Array(
    "org.wartremover.warts.JavaSerializable",
    "org.wartremover.warts.Serializable",
    "org.wartremover.warts.NonUnitStatements",
    "org.wartremover.warts.Product"
  )
)
class DataModelInstancesFilterSerializerTest extends AnyWordSpec with Matchers {

  import com.cognite.sdk.scala.v1.resources.DataModelInstances._

  "DataModelFilterSerializer" when {
    "encode LeafFilter" should {
      "work for equals filter" in {
        val equalInt = DMIEqualsFilter(Seq("name", "tag"), Json.fromInt(1)).asJson
        equalInt.toString() shouldBe """{
                                       |  "property" : [
                                       |    "name",
                                       |    "tag"
                                       |  ],
                                       |  "value" : 1
                                       |}""".stripMargin

        val equalString = DMIEqualsFilter(Seq("name", "tag"), Json.fromString("abcdef")).asJson
        equalString.toString() shouldBe """{
                                          |  "property" : [
                                          |    "name",
                                          |    "tag"
                                          |  ],
                                          |  "value" : "abcdef"
                                          |}""".stripMargin

        val equalBool = DMIEqualsFilter(Seq("name", "tag"), Json.fromBoolean(false)).asJson
        equalBool.toString() shouldBe """{
                                        |  "property" : [
                                        |    "name",
                                        |    "tag"
                                        |  ],
                                        |  "value" : false
                                        |}""".stripMargin
      }
      "work for in filter" in {
        val in = DMIInFilter(
          Seq("name", "tag"),
          Seq(
            Json.fromInt(1),
            Json.fromString("abcdef"),
            Json.fromBoolean(false),
            Json.fromFloatOrNull(2.64f)
          )
        ).asJson
        in.toString() shouldBe """{
                                 |  "property" : [
                                 |    "name",
                                 |    "tag"
                                 |  ],
                                 |  "values" : [
                                 |    1,
                                 |    "abcdef",
                                 |    false,
                                 |    2.64
                                 |  ]
                                 |}""".stripMargin

      }
      "work for range filter" in {
        the[IllegalArgumentException] thrownBy DMIRangeFilter(
          Seq("name", "tag")
        )

        the[IllegalArgumentException] thrownBy DMIRangeFilter(
          Seq("name", "tag"),
          gte = Some(Json.fromInt(1)),
          gt = Some(Json.fromInt(2))
        )

        the[IllegalArgumentException] thrownBy DMIRangeFilter(
          Seq("name", "tag"),
          lte = Some(Json.fromString("abc")),
          lt = Some(Json.fromString("def"))
        )

        val range =
          DMIRangeFilter(
            Seq("name", "tag"),
            gte = Some(Json.fromInt(1)),
            lt = Some(Json.fromInt(2))
          ).asJson

        range.toString() shouldBe """{
                                    |  "property" : [
                                    |    "name",
                                    |    "tag"
                                    |  ],
                                    |  "gte" : 1,
                                    |  "lt" : 2
                                    |}""".stripMargin

      }
      "work for prefix filter" in {
        val prefix = DMIPrefixFilter(Seq("name", "tag"), Json.fromString("abc")).asJson
        prefix.toString() shouldBe """{
                                 |  "property" : [
                                 |    "name",
                                 |    "tag"
                                 |  ],
                                 |  "value" : "abc"
                                 |}""".stripMargin

      }
      "work for exists filter" in {
        val exists = DMIExistsFilter(Seq("name", "tag")).asJson
        exists.toString() shouldBe """{
                                     |  "property" : [
                                     |    "name",
                                     |    "tag"
                                     |  ]
                                     |}""".stripMargin

      }
      "work for containsAny filter" in {
        val containsAny = DMIContainsAnyFilter(
          Seq("name", "tag"),
          Seq(
            Json.fromString("abcdef"),
            Json.fromFloatOrNull(2.64f)
          )
        ).asJson
        containsAny.toString() shouldBe """{
                                 |  "property" : [
                                 |    "name",
                                 |    "tag"
                                 |  ],
                                 |  "values" : [
                                 |    "abcdef",
                                 |    2.64
                                 |  ]
                                 |}""".stripMargin
      }
      "work for containsAll filter" in {
        val containsAll = DMIContainsAllFilter(
          Seq("name", "tag"),
          Seq(
            Json.fromInt(1),
            Json.fromBoolean(true)
          )
        ).asJson
        containsAll.toString() shouldBe """{
                                 |  "property" : [
                                 |    "name",
                                 |    "tag"
                                 |  ],
                                 |  "values" : [
                                 |    1,
                                 |    true
                                 |  ]
                                 |}""".stripMargin
      }
    }

    "encode BoolFilter" should {
      "work for and filter" in {
        val equalInt = DMIEqualsFilter(Seq("name", "tag"), Json.fromInt(1))
        val in = DMIInFilter(
          Seq("name", "tag"),
          Seq(
            Json.fromInt(1),
            Json.fromString("abcdef"),
            Json.fromBoolean(false),
            Json.fromFloatOrNull(2.64f)
          )
        )

        val and = DMIAndFilter(Seq(equalInt, in)).asJson
        and.toString() shouldBe """{
                                  |  "and" : [
                                  |    {
                                  |      "equals" : {
                                  |        "property" : [
                                  |          "name",
                                  |          "tag"
                                  |        ],
                                  |        "value" : 1
                                  |      }
                                  |    },
                                  |    {
                                  |      "in" : {
                                  |        "property" : [
                                  |          "name",
                                  |          "tag"
                                  |        ],
                                  |        "values" : [
                                  |          1,
                                  |          "abcdef",
                                  |          false,
                                  |          2.64
                                  |        ]
                                  |      }
                                  |    }
                                  |  ]
                                  |}""".stripMargin
      }
      "work for or filter" in {
        val range =
          DMIRangeFilter(
            Seq("name", "tag"),
            gte = Some(Json.fromInt(1)),
            lt = Some(Json.fromInt(2))
          )
        val prefix = DMIPrefixFilter(Seq("name", "tag"), Json.fromString("abc"))
        val exists = DMIExistsFilter(Seq("name", "tag"))

        val or = DMIOrFilter(Seq(range, prefix, exists)).asJson
        or.toString() shouldBe """{
                                 |  "or" : [
                                 |    {
                                 |      "range" : {
                                 |        "property" : [
                                 |          "name",
                                 |          "tag"
                                 |        ],
                                 |        "gte" : 1,
                                 |        "lt" : 2
                                 |      }
                                 |    },
                                 |    {
                                 |      "prefix" : {
                                 |        "property" : [
                                 |          "name",
                                 |          "tag"
                                 |        ],
                                 |        "value" : "abc"
                                 |      }
                                 |    },
                                 |    {
                                 |      "exists" : {
                                 |        "property" : [
                                 |          "name",
                                 |          "tag"
                                 |        ]
                                 |      }
                                 |    }
                                 |  ]
                                 |}""".stripMargin
      }
      "work for not filter" in {
        val containsAny = DMIContainsAnyFilter(
          Seq("name", "tag"),
          Seq(
            Json.fromString("abcdef"),
            Json.fromFloatOrNull(2.64f)
          )
        )

        val not = DMINotFilter(containsAny).asJson
        not.toString() shouldBe """{
                                  |  "not" : {
                                  |    "containsAny" : {
                                  |      "property" : [
                                  |        "name",
                                  |        "tag"
                                  |      ],
                                  |      "values" : [
                                  |        "abcdef",
                                  |        2.64
                                  |      ]
                                  |    }
                                  |  }
                                  |}""".stripMargin
      }

    }

    "encode a mix filter" should {
      "work for complex case" in {
        val equalInt = DMIEqualsFilter(Seq("name", "tag"), Json.fromInt(1))
        val in = DMIInFilter(
          Seq("name", "tag"),
          Seq(
            Json.fromInt(1),
            Json.fromString("abcdef"),
            Json.fromBoolean(false),
            Json.fromFloatOrNull(2.64f)
          )
        )
        val containsAny = DMIContainsAnyFilter(
          Seq("name", "tag"),
          Seq(
            Json.fromString("abcdef"),
            Json.fromFloatOrNull(2.64f)
          )
        )
        val orEqual = DMIOrFilter(Seq(equalInt))
        val orInContainsAny = DMIOrFilter(Seq(in, containsAny))

        val complex = DMIAndFilter(Seq(orEqual, orInContainsAny)).asJson
        complex.toString() shouldBe """{
                                  |  "and" : [
                                  |    {
                                  |      "or" : [
                                  |        {
                                  |          "equals" : {
                                  |            "property" : [
                                  |              "name",
                                  |              "tag"
                                  |            ],
                                  |            "value" : 1
                                  |          }
                                  |        }
                                  |      ]
                                  |    },
                                  |    {
                                  |      "or" : [
                                  |        {
                                  |          "in" : {
                                  |            "property" : [
                                  |              "name",
                                  |              "tag"
                                  |            ],
                                  |            "values" : [
                                  |              1,
                                  |              "abcdef",
                                  |              false,
                                  |              2.64
                                  |            ]
                                  |          }
                                  |        },
                                  |        {
                                  |          "containsAny" : {
                                  |            "property" : [
                                  |              "name",
                                  |              "tag"
                                  |            ],
                                  |            "values" : [
                                  |              "abcdef",
                                  |              2.64
                                  |            ]
                                  |          }
                                  |        }
                                  |      ]
                                  |    }
                                  |  ]
                                  |}""".stripMargin
      }
    }
  }

}
