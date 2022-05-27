// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.common

import com.cognite.sdk.scala.v1._
import io.circe
import io.circe.CursorOp.DownField
import io.circe.{Decoder, DecodingFailure, HCursor}
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

  val props: Map[String, DataModelPropertyDefinition] = Map(
    "prop_bool" -> DataModelPropertyDefinition(PropertyType.Boolean),
    "prop_float64" -> DataModelPropertyDefinition(PropertyType.Float64, false),
    "prop_string" -> DataModelPropertyDefinition(PropertyType.Text, false),
    "prop_direct_relation" -> DataModelPropertyDefinition(PropertyType.DirectRelation),
    "prop_date" -> DataModelPropertyDefinition(PropertyType.Date),
    "prop_timestamp" -> DataModelPropertyDefinition(PropertyType.Timestamp),
    "arr_bool" -> DataModelPropertyDefinition(PropertyType.Array.Boolean, false),
    "arr_float64" -> DataModelPropertyDefinition(PropertyType.Array.Float64, false),
    "arr_int32" -> DataModelPropertyDefinition(PropertyType.Array.Int),
    "arr_string" -> DataModelPropertyDefinition(PropertyType.Array.Text),
    "arr_empty" -> DataModelPropertyDefinition(PropertyType.Array.Text, false),
    "arr_empty_nullable" -> DataModelPropertyDefinition(PropertyType.Array.Float64)
  )

  import com.cognite.sdk.scala.v1.resources.Nodes._

  implicit val propertyTypeDecoder: Decoder[PropertyMap] =
    createDynamicPropertyDecoder(props)

  implicit val dataModelInstanceQueryResponseDecoder: Decoder[DataModelInstanceQueryResponse] =
    new Decoder[DataModelInstanceQueryResponse] {
      def apply(c: HCursor): Decoder.Result[DataModelInstanceQueryResponse] =
        for {
          items <- c.downField("items").as[Seq[PropertyMap]]
          modelProperties <- c.downField("modelProperties").as[Option[Map[String, DataModelPropertyDefinition]]]
          nextCursor <- c.downField("nextCursor").as[Option[String]]
        } yield DataModelInstanceQueryResponse(items, modelProperties, nextCursor)
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
        val res = decode[PropertyMap]("""{
                                      |    "prop_bool" : true,
                                      |    "prop_float64": 23.0,
                                      |    "prop_string": "toto",
                                      |    "prop_direct_relation": "Asset",
                                      |    "prop_date": "2022-03-22",
                                      |    "prop_timestamp": "2022-03-22T12:34:56.789+01:00",
                                      |    "arr_bool": [true, false, true],
                                      |    "arr_float64": [1.2, 2, 4.654],
                                      |    "arr_int32": [3, 1, 2147483646],
                                      |    "arr_int64": ["2147483650", "0", "9223372036854775", "1"],
                                      |    "arr_string": ["tata","titi"],
                                      |    "arr_empty": [],
                                      |    "arr_empty_nullable": []
                                      |}""".stripMargin)
        res.isRight shouldBe true

        val Right(dmiResponse) = res

        dmiResponse.allProperties.toSet shouldBe
          Set(
            "prop_bool" -> PropertyType.Boolean.Property(true),
            "prop_float64" -> PropertyType.Float64.Property(23.0),
            "prop_string" -> PropertyType.Text.Property("toto"),
            "prop_direct_relation" -> PropertyType.DirectRelation.Property("Asset"),
            "prop_date" -> PropertyType.Date.Property(LocalDate.of(2022, 3, 22)),
            "prop_timestamp" -> PropertyType.Timestamp.Property(
              ZonedDateTime.of(2022, 3, 22, 12, 34, 56, 789000000, ZoneOffset.of("+01:00"))
            ),
            "arr_bool" -> PropertyType.Array.Boolean.Property(
              Vector(true, false, true)
            ),
            "arr_float64" -> PropertyType.Array.Float64.Property(
              Vector(1.2, 2, 4.654)
            ),
            "arr_int32" -> PropertyType.Array.Int.Property(
              Vector(3, 1, 2147483646)
            ),
            "arr_int64" -> PropertyType.Array.Bigint.Property(
              Vector(2147483650L, 0, 9223372036854775L, 1).map(BigInt(_))
            ),
            "arr_string" -> PropertyType.Array.Text.Property(
              Vector("tata", "titi")
            ),
            "arr_empty" -> PropertyType.Array.Text.Property(Vector()),
            "arr_empty_nullable" -> PropertyType.Array.Float64.Property(Vector())
          )
      }
      "work for nullable property" in {
        val res = decode[PropertyMap]("""{
                                        |    "prop_float64": 23.0,
                                        |    "prop_string": "toto",
                                        |    "prop_direct_relation": "Asset",
                                        |    "arr_bool": [true, false, true],
                                        |    "arr_float64": [1.2, 2, 4.654],
                                        |    "arr_empty": []
                                        |}""".stripMargin)
        res.isRight shouldBe true

        val Right(dmiResponse) = res
        dmiResponse.allProperties.toSet shouldBe
          Set(
            "prop_float64" -> PropertyType.Float64.Property(23.0),
            "prop_string" -> PropertyType.Text.Property("toto"),
            "prop_direct_relation" -> PropertyType.DirectRelation.Property("Asset"),
            "arr_bool" -> PropertyType.Array.Boolean.Property(
              Vector(true, false, true)
            ),
            "arr_float64" -> PropertyType.Array.Float64.Property(
              Vector(1.2, 2, 4.654)
            ),
            "arr_empty" -> PropertyType.Array.Text.Property(Vector())
          )
      }

      "not work for primitive if given value does not match property type" in {
        val res: Either[circe.Error, DataModelInstanceQueryResponse] =
          decode[DataModelInstanceQueryResponse]("""{
                                        |"modelExternalId" : "tada",
                                        |"properties" : {
                                        |    "prop_bool" : "true",
                                        |    "prop_float64": 23.0,
                                        |    "prop_string": "toto",
                                        |    "arr_bool": [true, false],
                                        |    "arr_float64": [1.2, 2, 4.654],
                                        |    "arr_empty": []
                                        |} }""".stripMargin)

        checkErrorDecodingOnField(res, "prop_bool", "Boolean")
      }
      "not work for array if it contains Boolean and String" in {
        val res = decode[DataModelInstanceQueryResponse]("""{
                                        |"modelExternalId" : "tada",
                                        |"properties" : {
                                        |    "prop_bool" : true,
                                        |    "prop_float64": 23.0,
                                        |    "prop_string": "toto",
                                        |    "arr_bool": [true, "false", true],
                                        |    "arr_float64": [1.2, 2, 4.654],
                                        |    "arr_empty": []
                                        |} }""".stripMargin)
        checkErrorDecodingOnField(res, "arr_bool", "Boolean")
      }
      "not work for array if it contains Double and String" in {
        val res = decode[DataModelInstanceQueryResponse]("""{
                                        |"modelExternalId" : "tada",
                                        |"properties" : {
                                        |    "prop_bool" : true,
                                        |    "prop_float64": 23.0,
                                        |    "prop_string": "toto",
                                        |    "arr_bool": [true, false],
                                        |    "arr_float64": [1.2, 2.0, "abc"],
                                        |    "arr_empty": []
                                        |} }""".stripMargin)
        checkErrorDecodingOnField(res, "arr_float64", "Double")
      }
      "not work for array if it contains Double and Boolean" in {
        val res = decode[DataModelInstanceQueryResponse]("""{
                                        |"modelExternalId" : "tada",
                                        |"properties" : {
                                        |    "prop_bool" : true,
                                        |    "prop_float64": 23.0,
                                        |    "prop_string": "toto",
                                        |    "arr_bool": [true, false],
                                        |    "arr_float64": [false, 2.0, 3.6],
                                        |    "arr_empty": []
                                        |} }""".stripMargin)
        checkErrorDecodingOnField(res, "arr_float64", "Double")
      }
      "not work for property date if the string it not well formatted" in {
        val res = decode[DataModelInstanceQueryResponse]("""{
                                                           |"modelExternalId" : "tada",
                                                           |"properties" : {
                                                           |    "prop_float64": 23.0,
                                                           |    "prop_string": "toto",
                                                           |    "prop_date": "2022-02",
                                                           |    "arr_bool": [true, false, true],
                                                           |    "arr_float64": [1.2, 2, 4.654],
                                                           |    "arr_empty": []
                                                           |} }""".stripMargin)
        checkErrorDecodingOnField(res, "prop_date", "LocalDate")
      }
      "not work for property timestamp if the string it not well formatted" in {
        val res = decode[DataModelInstanceQueryResponse]("""{
                                                           |"modelExternalId" : "tada",
                                                           |"properties" : {
                                                           |    "prop_float64": 23.0,
                                                           |    "prop_string": "toto",
                                                           |    "prop_timestamp": "2022-02-03",
                                                           |    "arr_bool": [true, false, true],
                                                           |    "arr_float64": [1.2, 2, 4.654],
                                                           |    "arr_empty": []
                                                           |} }""".stripMargin)
        checkErrorDecodingOnField(res, "prop_timestamp", "ZonedDateTime")
      }
    }
    "encode PropertyType" should {
      import com.cognite.sdk.scala.v1.resources.Nodes.dataModelPropertyMapEncoder

      "work for primitive" in {
        val pm:PropertyMap = Node(
          "model_primitive",
          properties = Some(
            Map(
              "prop_bool" -> PropertyType.Boolean.Property(true),
              "prop_int32" -> PropertyType.Int.Property(123),
              "prop_int64" -> PropertyType.Bigint.Property(BigInt(9223372036854775L)),
              "prop_float32" -> PropertyType.Float32.Property(23.0f),
              "prop_float64" -> PropertyType.Float64.Property(23.0),
              "prop_string" -> PropertyType.Text.Property("toto"),
              "prop_direct_relation" -> PropertyType.DirectRelation.Property("Asset"),
              "prop_date" -> PropertyType.Date.Property(LocalDate.of(2022, 3, 22)),
              "prop_timestamp" -> PropertyType.Timestamp.Property(
                ZonedDateTime.of(2022, 3, 22, 12, 34, 56, 789000000, ZoneOffset.of("+01:00"))
              )
            )
          )
        )

        val res = pm.asJson.toString()

        val expectedJson1 = """{
                              |  "modelExternalId" : "model_primitive",
                              |  "properties" : {
                              |    "prop_float64" : 23.0,
                              |    "prop_int32" : 123,
                              |    "prop_string" : "toto",
                              |    "prop_timestamp" : "2022-03-22T12:34:56.789+01:00",
                              |    "prop_date" : "2022-03-22",
                              |    "prop_int64" : 9223372036854775,
                              |    "prop_bool" : true,
                              |    "prop_float32" : 23.0,
                              |    "prop_direct_relation" : "Asset"
                              |  }
                              |}""".stripMargin

        // Same content but order of properties in scala 2.12 is different than 2.13 and 3 ¯\_(ツ)_/¯
        val expectedJson2 = """{
                              |  "modelExternalId" : "model_primitive",
                              |  "properties" : {
                              |    "prop_float64" : 23.0,
                              |    "prop_float32" : 23.0,
                              |    "prop_direct_relation" : "Asset",
                              |    "prop_int32" : 123,
                              |    "prop_string" : "toto",
                              |    "prop_timestamp" : "2022-03-22T12:34:56.789+01:00",
                              |    "prop_date" : "2022-03-22",
                              |    "prop_int64" : 9223372036854775,
                              |    "prop_bool" : true
                              |  }
                              |}""".stripMargin

        res === expectedJson1 || res === expectedJson2 shouldBe true
      }
      "work for array" in {
        val pm: PropertyMap = Node(
          "model_array",
          properties = Some(
            Map(
              "arr_bool" -> PropertyType.Array.Boolean.Property(
                Vector(true, false, true)
              ),
              "arr_int32" -> PropertyType.Array.Int.Property(
                Vector(3, 1, 2147483646)
              ),
              "arr_int64" -> PropertyType.Array.Bigint.Property(
                Vector(2147483650L, 0, 9223372036854775L, 1).map(BigInt(_))
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

        pm.asJson.toString() shouldBe """{
                                         |  "modelExternalId" : "model_array",
                                         |  "properties" : {
                                         |    "arr_empty" : [
                                         |    ],
                                         |    "arr_int64" : [
                                         |      2147483650,
                                         |      0,
                                         |      9223372036854775,
                                         |      1
                                         |    ],
                                         |    "arr_string" : [
                                         |      "tata",
                                         |      "titi"
                                         |    ],
                                         |    "arr_int32" : [
                                         |      3,
                                         |      1,
                                         |      2147483646
                                         |    ],
                                         |    "arr_float32" : [
                                         |      2.3,
                                         |      6.35,
                                         |      7.48
                                         |    ],
                                         |    "arr_bool" : [
                                         |      true,
                                         |      false,
                                         |      true
                                         |    ],
                                         |    "arr_float64" : [
                                         |      1.2,
                                         |      2.0,
                                         |      4.654
                                         |    ]
                                         |  }
                                         |}""".stripMargin
      }
      "work for mixing both primitive and array" in {
        val res: PropertyMap = Node(
          "model_mixing",
          properties = Some(
            Map(
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
                Vector(2147483650L, 0, 9223372036854775L, 1).map(BigInt(_))
              ),
              "arr_string" -> PropertyType.Array.Text.Property(
                Vector("tata", "titi")
              ),
              "arr_empty" -> PropertyType.Array.Text.Property(Vector.empty)
            )
          )
        )
        res.asJson.toString() shouldBe """{
                                         |  "modelExternalId" : "model_mixing",
                                         |  "properties" : {
                                         |    "prop_float64" : 23.0,
                                         |    "arr_empty" : [
                                         |    ],
                                         |    "arr_int64" : [
                                         |      2147483650,
                                         |      0,
                                         |      9223372036854775,
                                         |      1
                                         |    ],
                                         |    "arr_string" : [
                                         |      "tata",
                                         |      "titi"
                                         |    ],
                                         |    "arr_int32" : [
                                         |      3,
                                         |      1,
                                         |      2147483646
                                         |    ],
                                         |    "prop_string" : "toto",
                                         |    "arr_bool" : [
                                         |      true,
                                         |      false,
                                         |      true
                                         |    ],
                                         |    "prop_bool" : true,
                                         |    "arr_float64" : [
                                         |      1.2,
                                         |      2.0,
                                         |      4.654
                                         |    ]
                                         |  }
                                         |}""".stripMargin
      }
    }
  }
}
