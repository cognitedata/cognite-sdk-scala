// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.common

import com.cognite.sdk.scala.v1.{
  ArrayProperty,
  BooleanProperty,
  DataModelInstanceCreate,
  DataModelInstanceQueryResponse,
  DataModelProperty,
  Float32Property,
  Float64Property,
  Int32Property,
  Int64Property,
  PropertyType,
  StringProperty
}
import io.circe
import io.circe.CursorOp.DownField
import io.circe.{Decoder, DecodingFailure, HCursor}
import io.circe.parser.decode
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
class DataModelInstancesSerializerTest extends AnyWordSpec with Matchers {

  val props: Map[String, DataModelProperty] = Map(
    "prop_bool" -> DataModelProperty("boolean"),
    "prop_float64" -> DataModelProperty("float64", false),
    "prop_string" -> DataModelProperty("text", false),
    "arr_bool" -> DataModelProperty("boolean[]", false),
    "arr_float64" -> DataModelProperty("float64[]", false),
    "arr_int32" -> DataModelProperty("int32[]"),
    "arr_int64" -> DataModelProperty("int64[]"),
    "arr_string" -> DataModelProperty("text[]"),
    "arr_empty" -> DataModelProperty("text[]", false),
    "arr_empty_nullable" -> DataModelProperty("float64[]")
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
    error.map(_._1) shouldBe Some(propType)
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
              "prop_string" -> StringProperty("toto")
            )
          )
        )
        res.asJson.toString() shouldBe """{
                                         |  "modelExternalId" : "model_primitive",
                                         |  "properties" : {
                                         |    "prop_float64" : 23.0,
                                         |    "prop_float32" : 23.0,
                                         |    "prop_int32" : 123,
                                         |    "prop_string" : "toto",
                                         |    "prop_int64" : 9223372036854775,
                                         |    "prop_bool" : true
                                         |  }
                                         |}""".stripMargin
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
