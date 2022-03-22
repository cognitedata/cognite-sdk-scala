// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.common

import com.cognite.sdk.scala.v1.{
  ArrayProperty,
  BooleanProperty,
  DataModelInstanceCreate,
  DataModelInstanceQueryResponse,
  DataModelProperty,
  DateProperty,
  DirectRelationProperty,
  Float32Property,
  Float64Property,
  Int32Property,
  Int64Property,
  PropertyName,
  PropertyType,
  StringProperty,
  TimeStampProperty
}
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
    "org.wartremover.warts.Product"
  )
)
class DataModelInstancesSerializerTest extends AnyWordSpec with Matchers {

  val props: Map[String, DataModelProperty] = Map(
    "prop_bool" -> DataModelProperty(PropertyName.boolean),
    "prop_float64" -> DataModelProperty(PropertyName.float64, false),
    "prop_string" -> DataModelProperty(PropertyName.text, false),
    "prop_direct_relation" -> DataModelProperty(PropertyName.directRelation),
    "prop_date" -> DataModelProperty(PropertyName.date),
    "prop_timestamp" -> DataModelProperty(PropertyName.timestamp),
    "arr_bool" -> DataModelProperty(PropertyName.arrayBoolean, false),
    "arr_float64" -> DataModelProperty(PropertyName.arrayFloat64, false),
    "arr_int32" -> DataModelProperty(PropertyName.arrayInt32),
    "arr_int64" -> DataModelProperty(PropertyName.arrayInt64),
    "arr_string" -> DataModelProperty(PropertyName.arrayText),
    "arr_empty" -> DataModelProperty(PropertyName.arrayText, false),
    "arr_empty_nullable" -> DataModelProperty(PropertyName.arrayFloat64)
  )

  import com.cognite.sdk.scala.v1.resources.DataModelInstances._

  implicit val propertyTypeDecoder: Decoder[Map[String, PropertyType]] =
    createDynamicPropertyDecoder(props)

  implicit val dataModelInstanceQueryResponseDecoder: Decoder[DataModelInstanceQueryResponse] =
    new Decoder[DataModelInstanceQueryResponse] {
      def apply(c: HCursor): Decoder.Result[DataModelInstanceQueryResponse] =
        for {
          modelExternalId <- c.downField("modelExternalId").as[String]
          properties <- c.downField("properties").as[Option[Map[String, PropertyType]]]
        } yield DataModelInstanceQueryResponse(modelExternalId, properties)
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

  "DataModelInstancesSerializer" when {
    "decode PropertyType" should {
      "work for primitive and array" in {
        val res = decode[DataModelInstanceQueryResponse]("""{
                                      |"modelExternalId" : "tada",
                                      |"properties" : {
                                      |    "prop_bool" : true,
                                      |    "prop_float64": 23.0,
                                      |    "prop_string": "toto",
                                      |    "prop_direct_relation": "Asset",
                                      |    "prop_date": "2022-03-22",
                                      |    "prop_timestamp": "2022-03-22T12:34:56.789+01:00",
                                      |    "arr_bool": [true, false, true],
                                      |    "arr_float64": [1.2, 2, 4.654],
                                      |    "arr_int32": [3, 1, 2147483646],
                                      |    "arr_int64": [2147483650, 0, 9223372036854775, 1],
                                      |    "arr_string": ["tata","titi"],
                                      |    "arr_empty": [],
                                      |    "arr_empty_nullable": []
                                      |} }""".stripMargin)
        res.isRight shouldBe true

        val Right(dmiResponse) = res

        dmiResponse shouldBe DataModelInstanceQueryResponse(
          "tada",
          Some(
            Map(
              "prop_bool" -> BooleanProperty(true),
              "prop_float64" -> Float64Property(23.0),
              "prop_string" -> StringProperty("toto"),
              "prop_direct_relation" -> DirectRelationProperty("Asset"),
              "prop_date" -> DateProperty(LocalDate.of(2022, 3, 22)),
              "prop_timestamp" -> TimeStampProperty(
                ZonedDateTime.of(2022, 3, 22, 12, 34, 56, 789000000, ZoneOffset.of("+01:00"))
              ),
              "arr_bool" -> ArrayProperty[BooleanProperty](
                Vector(true, false, true).map(BooleanProperty(_))
              ),
              "arr_float64" -> ArrayProperty[Float64Property](
                Vector(1.2, 2, 4.654).map(Float64Property(_))
              ),
              "arr_int32" -> ArrayProperty[Int32Property](
                Vector(3, 1, 2147483646).map(Int32Property(_))
              ),
              "arr_int64" -> ArrayProperty[Int64Property](
                Vector(2147483650L, 0, 9223372036854775L, 1).map(Int64Property(_))
              ),
              "arr_string" -> ArrayProperty[StringProperty](
                Vector("tata", "titi").map(StringProperty(_))
              ),
              "arr_empty" -> ArrayProperty(Vector()),
              "arr_empty_nullable" -> ArrayProperty(Vector())
            )
          )
        )
      }
      "work for nullable property" in {
        val res = decode[DataModelInstanceQueryResponse]("""{
                                        |"modelExternalId" : "tada",
                                        |"properties" : {
                                        |    "prop_float64": 23.0,
                                        |    "prop_string": "toto",
                                        |    "prop_direct_relation": "Asset",
                                        |    "arr_bool": [true, false, true],
                                        |    "arr_float64": [1.2, 2, 4.654],
                                        |    "arr_empty": []
                                        |} }""".stripMargin)
        res.isRight shouldBe true

        val Right(dmiResponse) = res
        dmiResponse shouldBe DataModelInstanceQueryResponse(
          "tada",
          Some(
            Map(
              "prop_float64" -> Float64Property(23.0),
              "prop_string" -> StringProperty("toto"),
              "prop_direct_relation" -> DirectRelationProperty("Asset"),
              "arr_bool" -> ArrayProperty[BooleanProperty](
                Vector(true, false, true).map(BooleanProperty(_))
              ),
              "arr_float64" -> ArrayProperty[Float64Property](
                Vector(1.2, 2, 4.654).map(Float64Property(_))
              ),
              "arr_empty" -> ArrayProperty(Vector())
            )
          )
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
      import com.cognite.sdk.scala.v1.resources.DataModelInstances.dataModelInstanceEncoder

      "work for primitive" in {
        val res = DataModelInstanceCreate(
          "model_primitive",
          Some(
            Map(
              "prop_bool" -> BooleanProperty(true),
              "prop_int32" -> Int32Property(123),
              "prop_int64" -> Int64Property(9223372036854775L),
              "prop_float32" -> Float32Property(23.0f),
              "prop_float64" -> Float64Property(23.0),
              "prop_string" -> StringProperty("toto"),
              "prop_direct_relation" -> DirectRelationProperty("Asset"),
              "prop_date" -> DateProperty(LocalDate.of(2022, 3, 22)),
              "prop_timestamp" -> TimeStampProperty(
                ZonedDateTime.of(2022, 3, 22, 12, 34, 56, 789000000, ZoneOffset.of("+01:00"))
              )
            )
          )
        ).asJson.toString()

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
        val res = DataModelInstanceCreate(
          "model_array",
          Some(
            Map(
              "arr_bool" -> ArrayProperty[BooleanProperty](
                Vector(true, false, true).map(BooleanProperty(_))
              ),
              "arr_int32" -> ArrayProperty[Int32Property](
                Vector(3, 1, 2147483646).map(Int32Property(_))
              ),
              "arr_int64" -> ArrayProperty[Int64Property](
                Vector(2147483650L, 0, 9223372036854775L, 1).map(Int64Property(_))
              ),
              "arr_float32" -> ArrayProperty[Float32Property](
                Vector(2.3f, 6.35f, 7.48f).map(Float32Property(_))
              ),
              "arr_float64" -> ArrayProperty[Float64Property](
                Vector(1.2, 2.0, 4.654).map(Float64Property(_))
              ),
              "arr_string" -> ArrayProperty[StringProperty](
                Vector("tata", "titi").map(StringProperty(_))
              ),
              "arr_empty" -> ArrayProperty[Int32Property](Vector.empty[Int32Property])
            )
          )
        )
        res.asJson.toString() shouldBe """{
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
        val res = DataModelInstanceCreate(
          "model_mixing",
          Some(
            Map(
              "prop_bool" -> BooleanProperty(true),
              "prop_float64" -> Float64Property(23.0),
              "prop_string" -> StringProperty("toto"),
              "arr_bool" -> ArrayProperty[BooleanProperty](
                Vector(true, false, true).map(BooleanProperty(_))
              ),
              "arr_float64" -> ArrayProperty[Float64Property](
                Vector(1.2, 2.0, 4.654).map(Float64Property(_))
              ),
              "arr_int32" -> ArrayProperty[Int32Property](
                Vector(3, 1, 2147483646).map(Int32Property(_))
              ),
              "arr_int64" -> ArrayProperty[Int64Property](
                Vector(2147483650L, 0, 9223372036854775L, 1).map(Int64Property(_))
              ),
              "arr_string" -> ArrayProperty[StringProperty](
                Vector("tata", "titi").map(StringProperty(_))
              ),
              "arr_empty" -> ArrayProperty[StringProperty](Vector.empty[StringProperty])
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
