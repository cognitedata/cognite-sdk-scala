// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1.containers

import io.circe._

sealed abstract class PropertyDefaultValue extends Product with Serializable

object PropertyDefaultValue {
  final case class String(value: java.lang.String) extends PropertyDefaultValue

  final case class Number(value: Double) extends PropertyDefaultValue

  final case class Boolean(value: scala.Boolean) extends PropertyDefaultValue

  final case class Object[T <: AnyRef](value: T)(implicit encoder: Encoder[T])
      extends PropertyDefaultValue {
    def toJson: Json = Json.fromString(encoder.apply(value).noSpaces)
  }

  implicit val propertyDefaultValueEncoder: Encoder[PropertyDefaultValue] =
    Encoder.instance[PropertyDefaultValue] {
      case PropertyDefaultValue.String(value) => Json.fromString(value)
      case PropertyDefaultValue.Number(value) => Json.fromDoubleOrString(value)
      case PropertyDefaultValue.Boolean(value) => Json.fromBoolean(value)
      case o @ PropertyDefaultValue.Object(_) => o.toJson
    }

  implicit val propertyDefaultValueDecoder: Decoder[PropertyDefaultValue] = { (c: HCursor) =>
    val result = c.value match {
      case v if v.isString =>
        v.asString.map(s =>
          Right[DecodingFailure, PropertyDefaultValue](PropertyDefaultValue.String(s))
        )
      case v if v.isNumber =>
        v.asNumber.map(n =>
          Right[DecodingFailure, PropertyDefaultValue](PropertyDefaultValue.Number(n.toDouble))
        )
      case v if v.isBoolean =>
        v.asBoolean.map(b =>
          Right[DecodingFailure, PropertyDefaultValue](PropertyDefaultValue.Boolean(b))
        )
      case v if v.isObject =>
        v.asObject.map(o =>
          Right[DecodingFailure, PropertyDefaultValue](
            PropertyDefaultValue.Object(Json.fromString(Json.fromJsonObject(o).noSpaces))
          )
        )
      case o =>
        Option(Left(DecodingFailure(s"Unknown Property Default Value :${o.noSpaces}", c.history)))
    }
    result.getOrElse(
      Left(DecodingFailure(s"Unknown Property Default Value :${c.value.noSpaces}", c.history))
    )
  }
}
