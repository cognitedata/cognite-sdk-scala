package com.cognite.sdk.scala.v1.fdm.instances

import com.cognite.sdk.scala.v1.fdm.common.filters.FilterValueDefinition.StringList
import com.cognite.sdk.scala.v1.fdm.common.filters.{FilterDefinition, FilterValueDefinition}
import com.cognite.sdk.scala.v1.fdm.containers.ContainerReference
import com.cognite.sdk.scala.v1.fdm.views.ViewReference
import io.circe.literal._
import io.circe.{Decoder, Json, Printer}
import io.circe.parser._
import io.circe.syntax._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

@SuppressWarnings(
  Array(
    "org.wartremover.warts.JavaSerializable",
    "org.wartremover.warts.Serializable",
    "org.wartremover.warts.NonUnitStatements",
    "org.wartremover.warts.Product",
    "org.wartremover.warts.AnyVal"
  )
)
class FilterDefinitionSerDeTest extends AnyWordSpec with Matchers {
  import com.cognite.sdk.scala.v1.fdm.common.filters.FilterDefinition._
  implicit val nullDroppingPrinter: Printer = Printer.noSpaces.copy(dropNullValues = true)

  private def checkPair(filter: FilterDefinition, json: Json) = {
    val filterJson = filter.asJson
    filterJson shouldBe json
    val jsonFilter = Decoder[FilterDefinition].decodeJson(json)
    jsonFilter shouldBe Right(filter)
  }

  "FilterDefinition Ser/de" when {
    "AndFilter" should {
      "encode-decode" in {
        checkPair(
          And(Seq(
            In(
              Seq("some_domain_model", "EntityTypeGroup", "entityType"),
              StringList(Seq("CFIHOS_00000003"))
            )
          )),
          json"""
          {
            "and": [{
              "in": {
                "property": [
                  "some_domain_model",
                  "EntityTypeGroup",
                  "entityType"
                ],
                "values": [
                  "CFIHOS_00000003"
                ]
              }
            }]
          }
        """
        )
      }
    }

    "NotFilter" should {
      "encode-decode" in {
        checkPair(
          Not(Not(
            In(
              Seq("some_domain_model", "EntityTypeGroup", "entityType"),
              StringList(Seq("CFIHOS_00000003"))
            )
          )),
          json"""
        {
          "not": {
            "not": {
              "in": {
                "property": [
                  "some_domain_model",
                  "EntityTypeGroup",
                  "entityType"
                ],
                "values": [
                  "CFIHOS_00000003"
                ]
              }
            }
          }
        }
      """
        )
      }
    }

    "LeafFilters" should {
      "work for equals filter" in {
        val equalInt = Equals(Seq("name", "tag"), FilterValueDefinition.Integer(1)).asJson
        equalInt.toString() shouldBe """{
                                       |  "property" : [
                                       |    "name",
                                       |    "tag"
                                       |  ],
                                       |  "value" : 1
                                       |}""".stripMargin

        val equalString =
          Equals(Seq("name", "tag"), FilterValueDefinition.String("abcdef")).asJson
        equalString.toString() shouldBe """{
                                          |  "property" : [
                                          |    "name",
                                          |    "tag"
                                          |  ],
                                          |  "value" : "abcdef"
                                          |}""".stripMargin

        val equalBool =
          Equals(Seq("name", "tag"), FilterValueDefinition.Boolean(false)).asJson
        equalBool.toString() shouldBe """{
                                        |  "property" : [
                                        |    "name",
                                        |    "tag"
                                        |  ],
                                        |  "value" : false
                                        |}""".stripMargin
      }

      "work for in filter" in {
        val in = In(Seq("name", "tag"), FilterValueDefinition.IntegerList(Seq(1L, 2L))).asJson
        Some(in) shouldBe parse("""{
                                  |  "property" : [
                                  |    "name",
                                  |    "tag"
                                  |  ],
                                  |  "values" : [
                                  |    1,
                                  |    2
                                  |  ]
                                  |}""".stripMargin).toOption

      }

      "work for range filter" in {
        the[IllegalArgumentException] thrownBy Range(
          Seq("name", "tag")
        )

        the[IllegalArgumentException] thrownBy Range(
          Seq("name", "tag"),
          gte = Some(FilterValueDefinition.Integer(1)),
          gt = Some(FilterValueDefinition.Integer(2))
        )

        the[IllegalArgumentException] thrownBy Range(
          Seq("name", "tag"),
          lte = Some(FilterValueDefinition.String("abc")),
          lt = Some(FilterValueDefinition.String("def"))
        )

        val range =
          Range(
            Seq("name", "tag"),
            gte = Some(FilterValueDefinition.Integer(1)),
            lt = Some(FilterValueDefinition.Integer(2))
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
        val prefix = Prefix(Seq("name", "tag"), FilterValueDefinition.String("abc")).asJson
        prefix.toString() shouldBe """{
                                     |  "property" : [
                                     |    "name",
                                     |    "tag"
                                     |  ],
                                     |  "value" : "abc"
                                     |}""".stripMargin
      }

      "work for exists filter" in {
        val exists = Exists(Seq("name", "tag")).asJson
        exists.toString() shouldBe """{
                                     |  "property" : [
                                     |    "name",
                                     |    "tag"
                                     |  ]
                                     |}""".stripMargin
      }

      "work for containsAny filter" in {
        val containsAny = ContainsAny(Seq("name", "tag"), FilterValueDefinition.StringList(Seq("abcdef", "pqrs"))).asJson
        containsAny.toString() shouldBe """{
                                          |  "property" : [
                                          |    "name",
                                          |    "tag"
                                          |  ],
                                          |  "values" : [
                                          |    "abcdef",
                                          |    "pqrs"
                                          |  ]
                                          |}""".stripMargin
      }

      "work for containsAll filter" in {
        val containsAll = ContainsAll(Seq("name", "tag"), FilterValueDefinition.IntegerList(Seq(123, 456))).asJson
        containsAll.toString() shouldBe """{
                                          |  "property" : [
                                          |    "name",
                                          |    "tag"
                                          |  ],
                                          |  "values" : [
                                          |    123,
                                          |    456
                                          |  ]
                                          |}""".stripMargin
      }

      "work for nested filter" in {
        val nested = Nested(
          Seq("some", "direct_relation", "property"),
          Equals(Seq("node", "name"), FilterValueDefinition.String("ACME"))
        ).asJson
        nested.toString() shouldBe """{
                                     |  "scope" : [
                                     |    "some",
                                     |    "direct_relation",
                                     |    "property"
                                     |  ],
                                     |  "filter" : {
                                     |    "equals" : {
                                     |      "property" : [
                                     |        "node",
                                     |        "name"
                                     |      ],
                                     |      "value" : "ACME"
                                     |    }
                                     |  }
                                     |}""".stripMargin
      }

      "work for overlaps filter" in {
        val overlaps = Overlaps(
          startProperty = Seq("room", "id"),
          endProperty = Seq("id"),
          gte = Some(FilterValueDefinition.Integer(10L))
        ).asJson
        overlaps.toString() shouldBe """{
                                       |  "startProperty" : [
                                       |    "room",
                                       |    "id"
                                       |  ],
                                       |  "endProperty" : [
                                       |    "id"
                                       |  ],
                                       |  "gte" : 10
                                       |}""".stripMargin
      }

      "work for hasData filter" in {
        val hasData = HasData(Seq(ContainerReference("space-1", "space-ext-id-1"), ViewReference("space-1", "view-ext-id-1", "v1"))).asJson
        hasData.toString() shouldBe """[
                                      |  {
                                      |    "type" : "container",
                                      |    "space" : "space-1",
                                      |    "externalId" : "space-ext-id-1"
                                      |  },
                                      |  {
                                      |    "type" : "view",
                                      |    "space" : "space-1",
                                      |    "externalId" : "view-ext-id-1",
                                      |    "version" : "v1"
                                      |  }
                                      |]""".stripMargin
      }
    }

    "BooleanFilters" should {
      "work for and filter" in {
        val equalInt = Equals(Seq("name", "tag"), FilterValueDefinition.Integer(1))
        val in = In(Seq("name", "tag"), FilterValueDefinition.StringList(Seq("abcdef", "lmnopqr")))

        val and = And(Seq(equalInt, in)).asJson
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
                                  |          "abcdef",
                                  |          "lmnopqr"
                                  |        ]
                                  |      }
                                  |    }
                                  |  ]
                                  |}""".stripMargin
      }
      "work for or filter" in {
        val range =
          Range(
            Seq("name", "tag"),
            gte = Some(FilterValueDefinition.Integer(1)),
            lt = Some(FilterValueDefinition.Integer(2))
          )
        val prefix = Prefix(Seq("name", "tag"), FilterValueDefinition.String("abc"))
        val exists = Exists(Seq("name", "tag"))

        val or = Or(Seq(range, prefix, exists)).asJson
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
        val containsAny = ContainsAny(Seq("name", "tag"), FilterValueDefinition.StringList(Seq("abcdef", "pqrs")))

        val not = Not(containsAny).asJson
        not.toString() shouldBe """{
                                  |  "not" : {
                                  |    "containsAny" : {
                                  |      "property" : [
                                  |        "name",
                                  |        "tag"
                                  |      ],
                                  |      "values" : [
                                  |        "abcdef",
                                  |        "pqrs"
                                  |      ]
                                  |    }
                                  |  }
                                  |}""".stripMargin
      }
    }

    "encode mixed filter" should {
      "work for complex case" in {
        val equalInt =
          Equals(Seq("name", "tag"), FilterValueDefinition.Integer(9223372036854775L))
        val in = In(Seq("name", "tag"), FilterValueDefinition.DoubleList(Seq(2.64, 1.23)))
        val containsAny = ContainsAny(Seq("name", "tag"), FilterValueDefinition.IntegerList(Seq(264, 123)))
        val orEqual = Or(Seq(equalInt))
        val orInContainsAny = Or(Seq(in, containsAny))

        val complex = And(Seq(orEqual, orInContainsAny)).asJson
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
                                      |            "value" : 9223372036854775
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
                                      |              2.64,
                                      |              1.23
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
                                      |              264,
                                      |              123
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
