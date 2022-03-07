// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.common

import com.cognite.sdk.scala.v1.{
  ArrayProperty,
  BooleanProperty,
  NumberProperty,
  PropertyType,
  StringProperty
}
import io.circe.generic.semiauto.deriveDecoder
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
    "org.wartremover.warts.Product"
  )
)
class DataModelInstancesSerializerTest extends AnyWordSpec with Matchers {

  import cats.syntax.functor._
  import io.circe.Decoder
  import io.circe.parser.decode

  implicit val dmiResponseDecoder: Decoder[DMIResponse] =
    deriveDecoder[DMIResponse]

  implicit val decodeEvent: Decoder[PropertyType] =
    List[Decoder[PropertyType]](
      Decoder.decodeBoolean.map(BooleanProperty).widen,
      Decoder.decodeDouble.map(NumberProperty).widen,
      Decoder.decodeString.map(StringProperty).widen,
      Decoder
        .decodeArray[Boolean]
        .map(x => ArrayProperty[BooleanProperty](x.map(BooleanProperty)))
        .widen,
      Decoder
        .decodeArray[Double]
        .map(x => ArrayProperty[NumberProperty](x.map(NumberProperty)))
        .widen,
      Decoder
        .decodeArray[String]
        .map(x => ArrayProperty[StringProperty](x.map(StringProperty)))
        .widen
    ).reduceLeft(_ or _)

  "DataModelInstancesSerializer" when {
    "decode PropertyType" should {
      "work for Boolean" in {
        val res = decode[DMIResponse]("""{"modelExternalId" : "tada",
                                      |"properties" : {
                                      |    "prop_bool" : true,
                                      |    "prop_number": 23.0,
                                      |    "prop_string": "toto",
                                      |    "arr_bool": [true, false, true],
                                      |    "arr_number": [1.2, 2, 4.654],
                                      |    "arr_string": ["tata","titi"]
                                      |} }""".stripMargin)
        res.isRight shouldBe true

        val Right(dmiResponse) = res

        dmiResponse.properties.map { prop =>
          prop.foreach { case (k, v) =>
            println(s" k = ${k} =>")
            v match {
              case arr: ArrayProperty[_] => arr.values.foreach(x => println(s"${x}, "))
              case x => println(x)
            }
          }
        }

        dmiResponse shouldBe DMIResponse(
          "tada",
          Some(
            Map(
              "prop_bool" -> BooleanProperty(true),
              "prop_number" -> NumberProperty(23.0),
              "prop_string" -> StringProperty("toto"),
              "arr_bool" -> ArrayProperty[BooleanProperty](
                Array(BooleanProperty(true), BooleanProperty(false), BooleanProperty(true))
              ),
              "arr_number" -> ArrayProperty[NumberProperty](
                Array(NumberProperty(1.2), NumberProperty(2), NumberProperty(4.654))
              ),
              "arr_string" -> ArrayProperty[StringProperty](
                Array(StringProperty("tata"), StringProperty("titi"))
              )
            )
          )
        )
      }
    }

  }

}
