// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.common

import com.cognite.sdk.scala.v1.{ArrayProperty, BooleanProperty, NumberProperty, PropertyType, StringProperty}
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

  implicit val decodeProp: Decoder[PropertyType] =
    List[Decoder[PropertyType]](
      Decoder.decodeBoolean.map(BooleanProperty(_)).widen,
      Decoder.decodeDouble.map(NumberProperty(_)).widen,
      Decoder.decodeString.map(StringProperty(_)).widen,
      Decoder
        .decodeArray[Boolean]
        .map(x => ArrayProperty[BooleanProperty](x.toVector.map(BooleanProperty(_))))
        .widen,
      Decoder
        .decodeArray[Double]
        .map(x => ArrayProperty[NumberProperty](x.toVector.map(NumberProperty(_))))
        .widen,
      Decoder
        .decodeArray[String]
        .map(x => ArrayProperty[StringProperty](x.toVector.map(StringProperty(_))))
        .widen
    ).reduceLeftOption(_ or _).getOrElse(Decoder.decodeString.map(StringProperty(_)).widen)

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

        dmiResponse shouldBe DMIResponse(
          "tada",
          Some(
            Map(
              "prop_bool" -> BooleanProperty(true),
              "prop_number" -> NumberProperty(23.0),
              "prop_string" -> StringProperty("toto"),
              "arr_bool" -> ArrayProperty[BooleanProperty](
                Vector(true,false,true).map(BooleanProperty(_))
              ),
              "arr_number" -> ArrayProperty[NumberProperty](
                Vector(1.2, 2, 4.654).map(NumberProperty(_))
              ),
              "arr_string" -> ArrayProperty[StringProperty](
                Vector("tata", "titi").map(StringProperty(_))
              )
            )
          )
        )
      }
    }

  }

}
