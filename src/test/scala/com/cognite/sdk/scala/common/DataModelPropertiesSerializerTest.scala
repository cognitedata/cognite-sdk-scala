// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.common

// scalastyle:off file.size.limit

import com.cognite.sdk.scala.v1._
import com.cognite.sdk.scala.v1.resources.DataModels
import io.circe
import io.circe.CursorOp.DownField
import io.circe.{Decoder, DecodingFailure, HCursor, Json}
import io.circe.parser.decode
import io.circe.syntax._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.time.{LocalDate, ZoneOffset, ZonedDateTime}

@SuppressWarnings(
  Array(
    "org.wartremover.warts.JavaSerializable",
    "org.wartremover.warts.Serializable",
    "org.wartremover.warts.NonUnitStatements",
    "org.wartremover.warts.Product",
    "org.wartremover.warts.AsInstanceOf",
    "org.wartremover.warts.IsInstanceOf"
  )
)
class DataModelPropertiesSerializerTest extends AnyWordSpec with Matchers {

  implicit val dataModelInstanceQueryResponseDecoder: Decoder[DataModelInstanceQueryResponse] = {
    import DataModels.dataModelPropertyDefinitionDecoder

    new Decoder[DataModelInstanceQueryResponse] {
      def apply(c: HCursor): Decoder.Result[DataModelInstanceQueryResponse] = {
        val modelProperties = c
          .downField("modelProperties")
          .as[Option[Map[String, DataModelPropertyDefinition]]]
        modelProperties.flatMap { props =>
          implicit val propertyTypeDecoder: Decoder[PropertyMap] =
            PropertyMap.createDynamicPropertyDecoder(props.getOrElse(Map()))
          for {
            items <- c.downField("items").as[Seq[PropertyMap]]
            nextCursor <- c.downField("nextCursor").as[Option[String]]
          } yield DataModelInstanceQueryResponse(items, props, nextCursor)
        }
      }
    }
  }

  private def checkErrorDecodingOnField(
      res: Either[circe.Error, DataModelInstanceQueryResponse],
      propName: String,
      propType: String
  ) = {
    res.isLeft shouldBe true
    val Left(decodingFailure) = res
    val error = DecodingFailure.unapply(decodingFailure)
    error.map(_._1).getOrElse("").contains(propType) shouldBe true
    val downFields = error
      .map(_._2)
      .getOrElse(List.empty[DownField])
      .filter(_.isInstanceOf[DownField])
      .map(_.asInstanceOf[DownField])
      .map(_.k)
    downFields.contains(propName) shouldBe true
  }

  "DataModelPropertiesSerializer" when {
    "decode PropertyType" should {
      "work for primitive and array" in {
        val res = decode[DataModelInstanceQueryResponse]("""{
                                        |    "items":
                                        |    [
                                        |        {
                                        |            "externalId": "tata",
                                        |            "prop_bool": true,
                                        |            "prop_float64": 23.0,
                                        |            "prop_string": "toto",
                                        |            "prop_direct_relation":
                                        |            [
                                        |              "spaceExternalId",
                                        |              "externalId"
                                        |            ],
                                        |            "prop_date": "2022-03-22",
                                        |            "prop_timestamp": "2022-03-22T12:34:56.789+01:00",
                                        |            "arr_bool":
                                        |            [
                                        |                true,
                                        |                false,
                                        |                true
                                        |            ],
                                        |            "arr_float64":
                                        |            [
                                        |                1.2,
                                        |                2,
                                        |                4.654
                                        |            ],
                                        |            "arr_int":
                                        |            [
                                        |                3,
                                        |                1,
                                        |                2147483646
                                        |            ],
                                        |            "arr_string":
                                        |            [
                                        |                "tata",
                                        |                "titi"
                                        |            ],
                                        |            "arr_empty":
                                        |            [],
                                        |            "arr_empty_nullable":
                                        |            []
                                        |        }
                                        |    ],
                                        |    "modelProperties":
                                        |    {
                                        |        "externalId":
                                        |        {
                                        |            "type": "text",
                                        |            "nullable": false
                                        |        },
                                        |        "prop_bool":
                                        |        {
                                        |            "type": "boolean",
                                        |            "nullable": true
                                        |        },
                                        |        "prop_float64":
                                        |        {
                                        |            "type": "float64",
                                        |            "nullable": false
                                        |        },
                                        |        "prop_string":
                                        |        {
                                        |            "type": "text",
                                        |            "nullable": false
                                        |        },
                                        |        "prop_direct_relation":
                                        |        {
                                        |            "type": "direct_relation",
                                        |            "nullable": true
                                        |        },
                                        |        "prop_date":
                                        |        {
                                        |            "type": "date",
                                        |            "nullable": true
                                        |        },
                                        |        "prop_timestamp":
                                        |        {
                                        |            "type": "timestamp",
                                        |            "nullable": true
                                        |        },
                                        |        "arr_bool":
                                        |        {
                                        |            "type": "boolean[]",
                                        |            "nullable": false
                                        |        },
                                        |        "arr_float64":
                                        |        {
                                        |            "type": "float64[]",
                                        |            "nullable": false
                                        |        },
                                        |        "arr_int":
                                        |        {
                                        |            "type": "int[]",
                                        |            "nullable": true
                                        |        },
                                        |        "arr_string":
                                        |        {
                                        |            "type": "text[]",
                                        |            "nullable": true
                                        |        },
                                        |        "arr_empty":
                                        |        {
                                        |            "type": "text[]",
                                        |            "nullable": false
                                        |        },
                                        |        "arr_empty_nullable":
                                        |        {
                                        |            "type": "float64[]",
                                        |            "nullable": true
                                        |        }
                                        |    }
                                        |}""".stripMargin)
        res.isRight shouldBe true

        val Right(dmiResponse) = res
        val expected = Map(
          "externalId" -> PropertyType.Text.Property("tata"),
          "prop_bool" -> PropertyType.Boolean.Property(true),
          "prop_float64" -> PropertyType.Float64.Property(23.0),
          "prop_string" -> PropertyType.Text.Property("toto"),
          "prop_direct_relation" -> PropertyType.DirectRelation.Property(List("spaceExternalId", "externalId")),
          "prop_date" -> PropertyType.Date.Property(LocalDate.of(2022, 3, 22)),
          "prop_timestamp" -> PropertyType.Timestamp.Property(
            ZonedDateTime.of(2022, 3, 22, 12, 34, 56, 789000000, ZoneOffset.of("+01:00"))
          ),
          "arr_bool" -> PropertyType.Array.Boolean.Property(
            List(true, false, true)
          ),
          "arr_float64" -> PropertyType.Array.Float64.Property(
            List(1.2, 2, 4.654)
          ),
          "arr_int" -> PropertyType.Array.Int.Property(
            List(3, 1, 2147483646)
          ),
          "arr_string" -> PropertyType.Array.Text.Property(
            List("tata", "titi")
          ),
          "arr_empty" -> PropertyType.Array.Text.Property(List.empty[String]),
          "arr_empty_nullable" -> PropertyType.Array.Float64.Property(List.empty[Double])
        )
        dmiResponse.items
          .map(_.allProperties)
          .headOption shouldBe Some(expected)
      }
      "work for nullable property" in {
        val res = decode[DataModelInstanceQueryResponse]("""{
                                                           |    "items":
                                                           |    [
                                                           |        {
                                                           |            "externalId": "test",
                                                           |            "prop_float64": 23.0,
                                                           |            "prop_string": "toto",
                                                           |            "prop_direct_relation":
                                                           |            [
                                                           |               "spaceExternalId",
                                                           |               "externalId"
                                                           |            ],
                                                           |            "arr_bool":
                                                           |            [
                                                           |                true,
                                                           |                false,
                                                           |                true
                                                           |            ],
                                                           |            "arr_float64":
                                                           |            [
                                                           |                1.2,
                                                           |                2,
                                                           |                4.654
                                                           |            ],
                                                           |            "arr_empty":
                                                           |            []
                                                           |        }
                                                           |    ],
                                                           |    "modelProperties":
                                                           |    {
                                                           |        "externalId":
                                                           |        {
                                                           |            "type": "text",
                                                           |            "nullable": false
                                                           |        },
                                                           |        "prop_bool":
                                                           |        {
                                                           |            "type": "boolean",
                                                           |            "nullable": true
                                                           |        },
                                                           |        "prop_float64":
                                                           |        {
                                                           |            "type": "float64",
                                                           |            "nullable": false
                                                           |        },
                                                           |        "prop_string":
                                                           |        {
                                                           |            "type": "text",
                                                           |            "nullable": false
                                                           |        },
                                                           |        "prop_direct_relation":
                                                           |        {
                                                           |            "type": "direct_relation",
                                                           |            "nullable": true
                                                           |        },
                                                           |        "prop_date":
                                                           |        {
                                                           |            "type": "date",
                                                           |            "nullable": true
                                                           |        },
                                                           |        "prop_timestamp":
                                                           |        {
                                                           |            "type": "timestamp",
                                                           |            "nullable": true
                                                           |        },
                                                           |        "arr_bool":
                                                           |        {
                                                           |            "type": "boolean[]",
                                                           |            "nullable": false
                                                           |        },
                                                           |        "arr_float64":
                                                           |        {
                                                           |            "type": "float64[]",
                                                           |            "nullable": false
                                                           |        },
                                                           |        "arr_int":
                                                           |        {
                                                           |            "type": "int[]",
                                                           |            "nullable": true
                                                           |        },
                                                           |        "arr_string":
                                                           |        {
                                                           |            "type": "text[]",
                                                           |            "nullable": true
                                                           |        },
                                                           |        "arr_empty":
                                                           |        {
                                                           |            "type": "text[]",
                                                           |            "nullable": false
                                                           |        },
                                                           |        "arr_empty_nullable":
                                                           |        {
                                                           |            "type": "float64[]",
                                                           |            "nullable": true
                                                           |        }
                                                           |    }
                                                           |}""".stripMargin)
        res.isRight shouldBe true

        val Right(dmiResponse) = res
        val expected = Map(
          "externalId" -> PropertyType.Text.Property("test"),
          "prop_float64" -> PropertyType.Float64.Property(23.0),
          "prop_string" -> PropertyType.Text.Property("toto"),
          "prop_direct_relation" -> PropertyType.DirectRelation.Property(Seq("spaceExternalId", "externalId")),
          "arr_bool" -> PropertyType.Array.Boolean.Property(
            List(true, false, true)
          ),
          "arr_float64" -> PropertyType.Array.Float64.Property(
            List(1.2, 2, 4.654)
          ),
          "arr_empty" -> PropertyType.Array.Text.Property(List())
        )

        dmiResponse.items
          .map(_.allProperties)
          .headOption shouldBe Some(expected)
      }

      "not work for primitive if given value does not match property type" in {
        val res: Either[circe.Error, DataModelInstanceQueryResponse] =
          decode[DataModelInstanceQueryResponse]("""{
                                                   |    "items":
                                                   |    [
                                                   |        {
                                                   |            "externalId": "tada",
                                                   |            "prop_bool": "true",
                                                   |            "prop_string": "keke",
                                                   |            "prop_float64": 1.3,
                                                   |            "arr_bool":
                                                   |            [
                                                   |                true,
                                                   |                true
                                                   |            ],
                                                   |            "arr_float64":
                                                   |            [
                                                   |                1.2,
                                                   |                2.0
                                                   |            ],
                                                   |            "arr_empty":
                                                   |            []
                                                   |        }
                                                   |    ],
                                                   |    "modelProperties":
                                                   |    {
                                                   |        "externalId":
                                                   |        {
                                                   |            "type": "text",
                                                   |            "nullable": false
                                                   |        },
                                                   |        "prop_string":
                                                   |        {
                                                   |            "type": "text",
                                                   |            "nullable": false
                                                   |        },
                                                   |        "prop_bool":
                                                   |        {
                                                   |            "type": "boolean",
                                                   |            "nullable": true
                                                   |        },
                                                   |        "prop_float64":
                                                   |        {
                                                   |            "type": "float64",
                                                   |            "nullable": false
                                                   |        },
                                                   |        "arr_bool":
                                                   |        {
                                                   |            "type": "boolean[]",
                                                   |            "nullable": false
                                                   |        },
                                                   |        "arr_float64":
                                                   |        {
                                                   |            "type": "float64[]",
                                                   |            "nullable": false
                                                   |        },
                                                   |        "arr_empty":
                                                   |        {
                                                   |            "type": "float64[]",
                                                   |            "nullable": false
                                                   |        }
                                                   |    }
                                                   |}""".stripMargin)

        checkErrorDecodingOnField(res, "prop_bool", "Boolean")
      }
      "not work for array if it contains Boolean and String" in {
        val res = decode[DataModelInstanceQueryResponse]("""{
                                                           |    "items":
                                                           |    [
                                                           |        {
                                                           |            "externalId": "tada",
                                                           |            "prop_string": "keke",
                                                           |            "prop_float64": 1.3,
                                                           |            "arr_bool":
                                                           |            [
                                                           |                true,
                                                           |                "false",
                                                           |                true
                                                           |            ],
                                                           |            "arr_float64":
                                                           |            [
                                                           |                1.2,
                                                           |                2.0
                                                           |            ],
                                                           |            "arr_empty":
                                                           |            []
                                                           |        }
                                                           |    ],
                                                           |    "modelProperties":
                                                           |    {
                                                           |        "externalId":
                                                           |        {
                                                           |            "type": "text",
                                                           |            "nullable": false
                                                           |        },
                                                           |        "prop_string":
                                                           |        {
                                                           |            "type": "text",
                                                           |            "nullable": false
                                                           |        },
                                                           |        "prop_float64":
                                                           |        {
                                                           |            "type": "float64",
                                                           |            "nullable": false
                                                           |        },
                                                           |        "arr_bool":
                                                           |        {
                                                           |            "type": "boolean",
                                                           |            "nullable": false
                                                           |        },
                                                           |        "arr_float64":
                                                           |        {
                                                           |            "type": "float64[]",
                                                           |            "nullable": false
                                                           |        },
                                                           |        "arr_empty":
                                                           |        {
                                                           |            "type": "float64[]",
                                                           |            "nullable": false
                                                           |        }
                                                           |    }
                                                           |}""".stripMargin)
        checkErrorDecodingOnField(res, "arr_bool", "Boolean")
      }
      "not work for array if it contains Double and String" in {
        val res = decode[DataModelInstanceQueryResponse]("""{
                                                           |    "items":
                                                           |    [
                                                           |        {
                                                           |            "externalId": "tada",
                                                           |            "prop_string": "keke",
                                                           |            "prop_float64": 1.3,
                                                           |            "arr_bool":
                                                           |            [
                                                           |                true,
                                                           |                false
                                                           |            ],
                                                           |            "arr_float64":
                                                           |            [
                                                           |                1.2,
                                                           |                2.0,
                                                           |                "abc"
                                                           |            ],
                                                           |            "arr_empty":
                                                           |            []
                                                           |        }
                                                           |    ],
                                                           |    "modelProperties":
                                                           |    {
                                                           |        "externalId":
                                                           |        {
                                                           |            "type": "text",
                                                           |            "nullable": false
                                                           |        },
                                                           |        "prop_string":
                                                           |        {
                                                           |            "type": "text",
                                                           |            "nullable": false
                                                           |        },
                                                           |        "prop_float64":
                                                           |        {
                                                           |            "type": "float64",
                                                           |            "nullable": false
                                                           |        },
                                                           |        "arr_bool":
                                                           |        {
                                                           |            "type": "boolean[]",
                                                           |            "nullable": false
                                                           |        },
                                                           |        "arr_float64":
                                                           |        {
                                                           |            "type": "float64[]",
                                                           |            "nullable": false
                                                           |        },
                                                           |        "arr_empty":
                                                           |        {
                                                           |            "type": "float64[]",
                                                           |            "nullable": false
                                                           |        }
                                                           |    }
                                                           |}""".stripMargin)

        checkErrorDecodingOnField(res, "arr_float64", "Double")
      }
      "not work for array if it contains Double and Boolean" in {
        val res = decode[DataModelInstanceQueryResponse]("""{
                                                           |    "items":
                                                           |    [
                                                           |        {
                                                           |            "externalId": "tada",
                                                           |            "prop_bool": true,
                                                           |            "prop_float64": 23.0,
                                                           |            "prop_string": "toto",
                                                           |            "arr_bool":
                                                           |            [
                                                           |                true,
                                                           |                false
                                                           |            ],
                                                           |            "arr_float64":
                                                           |            [
                                                           |                false,
                                                           |                2.0,
                                                           |                3.6
                                                           |            ],
                                                           |            "arr_empty":
                                                           |            []
                                                           |        }
                                                           |    ],
                                                           |    "modelProperties":
                                                           |    {
                                                           |        "externalId":
                                                           |        {
                                                           |            "type": "text",
                                                           |            "nullable": false
                                                           |        },
                                                           |        "prop_float64":
                                                           |        {
                                                           |            "type": "float64",
                                                           |            "nullable": false
                                                           |        },
                                                           |        "prop_string":
                                                           |        {
                                                           |            "type": "text",
                                                           |            "nullable": false
                                                           |        },
                                                           |        "arr_bool":
                                                           |        {
                                                           |            "type": "boolean[]",
                                                           |            "nullable": false
                                                           |        },
                                                           |        "arr_float64":
                                                           |        {
                                                           |            "type": "float64[]",
                                                           |            "nullable": false
                                                           |        },
                                                           |        "arr_empty":
                                                           |        {
                                                           |            "type": "float64[]",
                                                           |            "nullable": false
                                                           |        }
                                                           |    }
                                                           |}""".stripMargin)
        checkErrorDecodingOnField(res, "arr_float64", "Double")
      }
      "not work for property date if the string it not well formatted" in {
        val res = decode[DataModelInstanceQueryResponse]("""{
                                                           |    "items":
                                                           |    [
                                                           |        {
                                                           |            "externalId": "tada",
                                                           |            "prop_float64": 23.0,
                                                           |            "prop_string": "toto",
                                                           |            "prop_date": "2022-02",
                                                           |            "arr_bool":
                                                           |            [
                                                           |                true,
                                                           |                false,
                                                           |                true
                                                           |            ],
                                                           |            "arr_float64":
                                                           |            [
                                                           |                1.2,
                                                           |                2,
                                                           |                4.654
                                                           |            ],
                                                           |            "arr_empty":
                                                           |            []
                                                           |        }
                                                           |    ],
                                                           |    "modelProperties":
                                                           |    {
                                                           |        "externalId":
                                                           |        {
                                                           |            "type": "text",
                                                           |            "nullable": false
                                                           |        },
                                                           |        "prop_float64":
                                                           |        {
                                                           |            "type": "float64",
                                                           |            "nullable": false
                                                           |        },
                                                           |        "prop_string":
                                                           |        {
                                                           |            "type": "text",
                                                           |            "nullable": false
                                                           |        },
                                                           |        "prop_date":
                                                           |        {
                                                           |            "type": "date",
                                                           |            "nullable": true
                                                           |        },
                                                           |        "arr_bool":
                                                           |        {
                                                           |            "type": "boolean[]",
                                                           |            "nullable": false
                                                           |        },
                                                           |        "arr_float64":
                                                           |        {
                                                           |            "type": "float64[]",
                                                           |            "nullable": false
                                                           |        },
                                                           |        "arr_empty":
                                                           |        {
                                                           |            "type": "float64[]",
                                                           |            "nullable": false
                                                           |        }
                                                           |    }
                                                           |}""".stripMargin)
        checkErrorDecodingOnField(res, "prop_date", "LocalDate")
      }
      "not work for property timestamp if the string it not well formatted" in {
        val res = decode[DataModelInstanceQueryResponse]("""{
                                                           |    "items":
                                                           |    [
                                                           |        {
                                                           |            "externalId": "tada",
                                                           |            "prop_float64": 23.0,
                                                           |            "prop_string": "toto",
                                                           |            "prop_timestamp": "2022-02-03",
                                                           |            "arr_bool":
                                                           |            [
                                                           |                true,
                                                           |                false,
                                                           |                true
                                                           |            ],
                                                           |            "arr_float64":
                                                           |            [
                                                           |                1.2,
                                                           |                2,
                                                           |                4.654
                                                           |            ],
                                                           |            "arr_empty":
                                                           |            []
                                                           |        }
                                                           |    ],
                                                           |    "modelProperties":
                                                           |    {
                                                           |        "externalId":
                                                           |        {
                                                           |            "type": "text",
                                                           |            "nullable": false
                                                           |        },
                                                           |        "prop_float64":
                                                           |        {
                                                           |            "type": "float64",
                                                           |            "nullable": false
                                                           |        },
                                                           |        "prop_string":
                                                           |        {
                                                           |            "type": "text",
                                                           |            "nullable": false
                                                           |        },
                                                           |        "prop_timestamp":
                                                           |        {
                                                           |            "type": "timestamp",
                                                           |            "nullable": true
                                                           |        },
                                                           |        "arr_bool":
                                                           |        {
                                                           |            "type": "boolean[]",
                                                           |            "nullable": false
                                                           |        },
                                                           |        "arr_float64":
                                                           |        {
                                                           |            "type": "float64[]",
                                                           |            "nullable": false
                                                           |        },
                                                           |        "arr_empty":
                                                           |        {
                                                           |            "type": "float64[]",
                                                           |            "nullable": false
                                                           |        }
                                                           |    }
                                                           |}""".stripMargin)
        checkErrorDecodingOnField(res, "prop_timestamp", "ZonedDateTime")
      }
    }
    "encode PropertyType" should {
      import PropertyMap.dataModelPropertyMapEncoder

      "work for primitive" in {
        val pm:PropertyMap = Node(
          "model_primitive",
          properties = Some(
            Map(
              "externalId" -> PropertyType.Text.Property("model_primitive"),
              "prop_bool" -> PropertyType.Boolean.Property(true),
              "prop_int32" -> PropertyType.Int.Property(123),
              "prop_int64" -> PropertyType.Bigint.Property(9223372036854775L),
              "prop_float32" -> PropertyType.Float32.Property(23.0f),
              "prop_float64" -> PropertyType.Float64.Property(23.0),
              "prop_string" -> PropertyType.Text.Property("toto"),
              "prop_direct_relation" -> PropertyType.DirectRelation.Property(List("spaceExternalId", "externalId")),
              "prop_date" -> PropertyType.Date.Property(LocalDate.of(2022, 3, 22)),
              "prop_timestamp" -> PropertyType.Timestamp.Property(
                ZonedDateTime.of(2022, 3, 22, 12, 34, 56,
                  789000000, ZoneOffset.of("+01:00"))
              )
            )
          )
        )

        val res = pm.asJson.spaces2SortKeys

        val expectedJsonObj = Json.obj(
          ("externalId", Json.fromString("model_primitive")),
          ("prop_float64", Json.fromDoubleOrNull(23.0)),
          ("prop_float32", Json.fromDoubleOrNull(23.0)),
          ("prop_direct_relation", Json.fromValues(List(Json.fromString("spaceExternalId"), Json.fromString("externalId")))),
          ("prop_int32", Json.fromInt(123)),
          ("prop_string", Json.fromString("toto")),
          ("prop_timestamp", Json.fromString("2022-03-22T12:34:56.789+01:00")),
          ("prop_date", Json.fromString("2022-03-22")),
          ("prop_int64", Json.fromBigInt(9223372036854775L)),
          ("prop_bool", Json.fromBoolean(true))
        ).spaces2SortKeys

        res shouldBe expectedJsonObj
      }
      "work for array" in {
        val pm: PropertyMap = Node(
          "model_array",
          properties = Some(
            Map(
              "externalId" -> PropertyType.Text.Property("model_array"),
              "arr_bool" -> PropertyType.Array.Boolean.Property(
                Vector(true, false, true)
              ),
              "arr_int32" -> PropertyType.Array.Int.Property(
                Vector(3, 1, 2147483646)
              ),
              "arr_int64" -> PropertyType.Array.Bigint.Property(
                Vector(2147483650L, 0, 9223372036854775L, 1)
              ),
              "arr_float32" -> PropertyType.Array.Float32.Property(
                Vector(2.3f, 6.35f, 7.48f)
              ),
              "arr_float64" -> PropertyType.Array.Float64.Property(
                Vector(1.2, 2.0, 4.654)
              ),
              "arr_string" -> PropertyType.Array.Text.Property(
                Vector("tata", "titi")
              ),
              "arr_empty" -> PropertyType.Array.Int.Property(Vector.empty)
            )
          )
        )

        pm.asJson.spaces2SortKeys shouldBe Json.obj(
          ("externalId", Json.fromString("model_array")),
          ("arr_bool", Json.fromValues(List(Json.fromBoolean(true),
            Json.fromBoolean(false),
            Json.fromBoolean(true)))),
          ("arr_float32", Json.fromValues(List(Json.fromFloatOrNull(2.3f),
            Json.fromFloatOrNull(6.35f),
            Json.fromFloatOrNull(7.48f)))),
          ("arr_float64", Json.fromValues(List(Json.fromDoubleOrNull(1.2),
            Json.fromDoubleOrNull(2.0),
            Json.fromDoubleOrNull(4.654)))),
          ("arr_int32", Json.fromValues(List(Json.fromInt(3),
            Json.fromInt(1),
            Json.fromInt(2147483646)))),
          ("arr_int64", Json.fromValues(List(Json.fromBigInt(2147483650L),
            Json.fromBigInt(0), Json.fromBigInt(9223372036854775L), Json.fromBigInt(1)))),
          ("arr_string", Json.fromValues(List(Json.fromString("tata"), Json.fromString("titi")))),
          ("arr_empty", Json.fromValues(List()))
        ).spaces2SortKeys
      }
      "work for mixing both primitive and array" in {
        val res: PropertyMap = Node(
          "model_mixing",
          properties = Some(
            Map(
              "externalId" -> PropertyType.Text.Property("model_mixing"),
              "prop_bool" -> PropertyType.Boolean.Property(true),
              "prop_float64" -> PropertyType.Float64.Property(23.0),
              "prop_string" -> PropertyType.Text.Property("toto"),
              "arr_bool" -> PropertyType.Array.Boolean.Property(
                Vector(true, false, true)
              ),
              "arr_float64" -> PropertyType.Array.Float64.Property(
                Vector(1.2, 2.0, 4.654)
              ),
              "arr_int32" -> PropertyType.Array.Int.Property(
                Vector(3, 1, 2147483646)
              ),
              "arr_int64" -> PropertyType.Array.Bigint.Property(
                Vector(2147483650L, 0, 9223372036854775L, 1)
              ),
              "arr_string" -> PropertyType.Array.Text.Property(
                Vector("tata", "titi")
              ),
              "arr_empty" -> PropertyType.Array.Text.Property(Vector.empty)
            )
          )
        )
        res.asJson.spaces2SortKeys shouldBe Json.obj(
          ("externalId", Json.fromString("model_mixing")),
          ("prop_bool", Json.fromBoolean(true)),
          ("prop_float64", Json.fromDoubleOrNull(23.0)),
          ("prop_string", Json.fromString("toto")),
          ("arr_bool", Json.fromValues(List(Json.fromBoolean(true),
            Json.fromBoolean(false),
            Json.fromBoolean(true)))),
          ("arr_float64", Json.fromValues(List(Json.fromDoubleOrNull(1.2),
            Json.fromDoubleOrNull(2.0),
            Json.fromDoubleOrNull(4.654)))),
          ("arr_int32", Json.fromValues(List(Json.fromInt(3),
            Json.fromInt(1),
            Json.fromInt(2147483646)))),
          ("arr_int64", Json.fromValues(List(Json.fromBigInt(2147483650L),
            Json.fromBigInt(0), Json.fromBigInt(9223372036854775L), Json.fromBigInt(1)))),
          ("arr_string", Json.fromValues(List(Json.fromString("tata"), Json.fromString("titi")))),
          ("arr_empty", Json.fromValues(List()))
        ).spaces2SortKeys
      }
    }
  }
}
