// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.common

import com.cognite.sdk.scala.v1._
import io.circe.syntax._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import io.circe.parser._

@SuppressWarnings(
  Array(
    "org.wartremover.warts.JavaSerializable",
    "org.wartremover.warts.Serializable",
    "org.wartremover.warts.NonUnitStatements",
    "org.wartremover.warts.Product",
    "org.wartremover.warts.AnyVal"
  )
)
class DomainSpecificLanguageFilterSerializerTest extends AnyWordSpec with Matchers {

  import com.cognite.sdk.scala.v1.resources.Nodes._

  "DataModelFilterSerializer" when {
    "encode EmptyFilter" should {
      "return an empty object"in {
        val empty:DomainSpecificLanguageFilter = EmptyFilter
        empty.asJson.toString() shouldBe """{
                                       |  
                                       |}""".stripMargin
      }
    }
    "encode LeafFilter" should {
      "work for equals filter" in {
        val equalInt = DMIEqualsFilter(Seq("name", "tag"), PropertyType.Int.Property(1)).asJson
        equalInt.toString() shouldBe """{
                                       |  "property" : [
                                       |    "name",
                                       |    "tag"
                                       |  ],
                                       |  "value" : 1
                                       |}""".stripMargin

        val equalString = DMIEqualsFilter(Seq("name", "tag"), PropertyType.Text.Property("abcdef")).asJson
        equalString.toString() shouldBe """{
                                          |  "property" : [
                                          |    "name",
                                          |    "tag"
                                          |  ],
                                          |  "value" : "abcdef"
                                          |}""".stripMargin

        val equalBool = DMIEqualsFilter(Seq("name", "tag"), PropertyType.Boolean.Property(false)).asJson
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
            PropertyType.Bigint.Property(BigInt("123456789123456789123456789")),
            PropertyType.Text.Property("abcdef"),
            PropertyType.Boolean.Property(false),
            PropertyType.Float32.Property(2.64f)
          )
        ).asJson
        Some(in) shouldBe parse("""{
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
                                 |}""".stripMargin).toOption

      }
      "work for range filter" in {
        the[IllegalArgumentException] thrownBy DMIRangeFilter(
          Seq("name", "tag")
        )

        the[IllegalArgumentException] thrownBy DMIRangeFilter(
          Seq("name", "tag"),
          gte = Some(PropertyType.Int.Property(1)),
          gt = Some(PropertyType.Int.Property(2))
        )

        the[IllegalArgumentException] thrownBy DMIRangeFilter(
          Seq("name", "tag"),
          lte = Some(PropertyType.Text.Property("abc")),
          lt = Some(PropertyType.Text.Property("def"))
        )

        val range =
          DMIRangeFilter(
            Seq("name", "tag"),
            gte = Some(PropertyType.Int.Property(1)),
            lt = Some(PropertyType.Int.Property(2))
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
        val prefix = DMIPrefixFilter(Seq("name", "tag"), PropertyType.Text.Property("abc")).asJson
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
            PropertyType.Text.Property("abcdef"),
            PropertyType.Float32.Property(2.64f)
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
            PropertyType.Int.Property(1),
            PropertyType.Boolean.Property(true)
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
        val equalInt = DMIEqualsFilter(Seq("name", "tag"), PropertyType.Int.Property(1))
        val in = DMIInFilter(
          Seq("name", "tag"),
          Seq(
            PropertyType.Int.Property(1),
            PropertyType.Text.Property("abcdef"),
            PropertyType.Boolean.Property(false),
            PropertyType.Float64.Property(2.64)
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
            gte = Some(PropertyType.Int.Property(1)),
            lt = Some(PropertyType.Int.Property(2))
          )
        val prefix = DMIPrefixFilter(Seq("name", "tag"), PropertyType.Text.Property("abc"))
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
            PropertyType.Text.Property("abcdef"),
            PropertyType.Float32.Property(2.64f)
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
        val equalInt = DMIEqualsFilter(Seq("name", "tag"), PropertyType.Bigint.Property(BigInt("123456789123456789123456789")))
        val in = DMIInFilter(
          Seq("name", "tag"),
          Seq(
            PropertyType.Bigint.Property(BigInt("123456789123456789123456789")),
            PropertyType.Text.Property("abcdef"),
            PropertyType.Boolean.Property(false),
            PropertyType.Float32.Property(2.64f)
          )
        )
        val containsAny = DMIContainsAnyFilter(
          Seq("name", "tag"),
          Seq(
            PropertyType.Text.Property("abcdef"),
            PropertyType.Float32.Property(2.64f)
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
