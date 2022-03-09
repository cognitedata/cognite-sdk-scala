// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.common

import com.cognite.sdk.scala.v1.{
  ArrayProperty,
  BooleanProperty,
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
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

final case class DMIResponse(
    modelExternalId: String,
    properties: Option[Map[String, PropertyType]] = None
)

@SuppressWarnings(
  Array(
    "org.wartremover.warts.JavaSerializable",
    "org.wartremover.warts.Serializable",
    "org.wartremover.warts.NonUnitStatements",
    "org.wartremover.warts.PublicInference",
    "org.wartremover.warts.Product"
  )
)
class DataModelInstancesSerializerTest extends AnyWordSpec with Matchers {

  val props = Map(
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

  implicit val decodeDMIResponse: Decoder[DMIResponse] = new Decoder[DMIResponse] {
    def apply(c: HCursor): Decoder.Result[DMIResponse] =
      for {
        modelExternalId <- c.downField("modelExternalId").as[String]
        properties <- c.downField("properties").as[Option[Map[String, PropertyType]]]
      } yield DMIResponse(modelExternalId, properties)
  }

  // scalastyle:off cyclomatic.complexity
  private def decodeBaseOnType(c: HCursor, propName: String, propType: String) =
    propType match {
      case "boolean" => c.downField(propName).as[Boolean]
      case "int" | "int32" => c.downField(propName).as[Int]
      case "bigint" | "int64" => c.downField(propName).as[Long]
      case "float32" => c.downField(propName).as[Float]
      case "float64" => c.downField(propName).as[Double]
      case "text" => c.downField(propName).as[String]
      case "boolean[]" => c.downField(propName).as[Vector[Boolean]]
      case "int[]" | "int32[]" => c.downField(propName).as[Vector[Int]]
      case "bigint[]" | "int64[]" => c.downField(propName).as[Vector[Long]]
      case "float32[]" => c.downField(propName).as[Vector[Float]]
      case "float64[]" => c.downField(propName).as[Vector[Double]]
      case "text[]" => c.downField(propName).as[Vector[String]]
    }
  // scalastyle:on cyclomatic.complexity

  private def decodeArrayFromTypeOfFirstElement(c: Vector[_], propName: String) =
    c.headOption match {
      case Some(_: Boolean) =>
        propName -> ArrayProperty[BooleanProperty](
          c.map(_.asInstanceOf[Boolean]).map(BooleanProperty(_))
        )
      case Some(_: Int) =>
        propName -> ArrayProperty[Int32Property](
          c.map(_.asInstanceOf[Int]).map(Int32Property(_))
        )
      case Some(_: Long) =>
        propName -> ArrayProperty[Int64Property](
          c.map(_.asInstanceOf[Long]).map(Int64Property(_))
        )
      case Some(_: Float) =>
        propName -> ArrayProperty[Float32Property](
          c.map(_.asInstanceOf[Float]).map(Float32Property(_))
        )
      case Some(_: Double) =>
        propName -> ArrayProperty[Float64Property](
          c.map(_.asInstanceOf[Double]).map(Float64Property(_))
        )
      case Some(_: String) =>
        propName -> ArrayProperty[StringProperty](
          c.map(_.asInstanceOf[String]).map(StringProperty(_))
        )
      case _ => propName -> ArrayProperty(Vector())
    }

  private def filterOutNullableProps(
      res: Iterable[Either[DecodingFailure, (String, PropertyType)]]
  ): Iterable[Either[DecodingFailure, (String, PropertyType)]] =
    res
      .filter {
        case Right(_) => true
        case Left(DecodingFailure("Attempt to decode value on failed cursor", downfields)) =>
          val nullableProps = downfields
            .filter(_.isInstanceOf[DownField])
            .map(_.asInstanceOf[DownField])
            .map(_.k)
            .filter(_ !== "properties")
            .toSet
          !nullableProps.subsetOf(props.filter(_._2.nullable).keySet)
        case _ => true
      }

  // scalastyle:off cyclomatic.complexity
  implicit val decodeVinDecodeDetails: Decoder[Map[String, PropertyType]] =
    new Decoder[Map[String, PropertyType]] {
      def apply(c: HCursor): Decoder.Result[Map[String, PropertyType]] = {
        val res = props.map { case (prop, dmp) =>
          for {
            value <- decodeBaseOnType(c, prop, dmp.`type`)
          } yield value match {
            case b: Boolean => prop -> BooleanProperty(b)
            case i: Int => prop -> Int32Property(i)
            case l: Long => prop -> Int64Property(l)
            case f: Float => prop -> Float32Property(f)
            case d: Double => prop -> Float64Property(d)
            case s: String => prop -> StringProperty(s)
            case v: Vector[_] =>
              decodeArrayFromTypeOfFirstElement(v, prop)
            case invalidValue =>
              throw new Exception(s"${invalidValue.toString} does not match any property type")
          }
        }
        filterOutNullableProps(res).find(_.isLeft) match {
          case Some(Left(x)) => Left(x)
          case _ => Right(res.collect { case Right(value) => value }.toMap)
        }
      }
    }
  // scalastyle:on cyclomatic.complexity

  private def checkErrorDecodingOnField(
      res: Either[circe.Error, DMIResponse],
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
        val res = decode[DMIResponse]("""{
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

        dmiResponse shouldBe DMIResponse(
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
        val res = decode[DMIResponse]("""{
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
        dmiResponse shouldBe DMIResponse(
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
        val res: Either[circe.Error, DMIResponse] = decode[DMIResponse]("""{
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
        val res = decode[DMIResponse]("""{
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
        val res = decode[DMIResponse]("""{
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
        val res = decode[DMIResponse]("""{
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
  }
}
