// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1.fdm.common

import io.circe._

sealed abstract class PropertyDefaultValue extends Product with Serializable

object PropertyDefaultValue {
  final case class String(value: java.lang.String) extends PropertyDefaultValue

  final case class Integer(value: scala.Long) extends PropertyDefaultValue

  final case class Double(value: scala.Double) extends PropertyDefaultValue

  final case class Boolean(value: scala.Boolean) extends PropertyDefaultValue

  final case class Object(value: Json) extends PropertyDefaultValue

  implicit val propertyDefaultValueEncoder: Encoder[PropertyDefaultValue] =
    Encoder.instance[PropertyDefaultValue] {
      case PropertyDefaultValue.String(value) => Json.fromString(value)
      case PropertyDefaultValue.Integer(value) => Json.fromLong(value)
      case PropertyDefaultValue.Double(value) => Json.fromDoubleOrString(value)
      case PropertyDefaultValue.Boolean(value) => Json.fromBoolean(value)
      case PropertyDefaultValue.Object(value) => value
    }

  implicit val propertyDefaultValueDecoder: Decoder[PropertyDefaultValue] = { (c: HCursor) =>
    val result = c.value match {
      case v if v.isString =>
        v.asString.map(s =>
          Right[DecodingFailure, PropertyDefaultValue](PropertyDefaultValue.String(s))
        )
      case v if v.isNumber =>
        val numericPropertyValue = v.asNumber.flatMap { jn =>
          if (jn.toString.contains(".")) {
            Some(PropertyDefaultValue.Double(jn.toDouble))
          } else {
            jn.toLong.map(PropertyDefaultValue.Integer.apply)
          }
        }
        numericPropertyValue.map(Right[DecodingFailure, PropertyDefaultValue])
      case v if v.isBoolean =>
        v.asBoolean.map(b =>
          Right[DecodingFailure, PropertyDefaultValue](PropertyDefaultValue.Boolean(b))
        )
      case v if v.isObject =>
        Some(Right[DecodingFailure, PropertyDefaultValue](PropertyDefaultValue.Object(v)))
      case o =>
        Some(Left(DecodingFailure(s"Unknown Property Default Value :${o.noSpaces}", c.history)))
    }
    result.getOrElse(
      Left(DecodingFailure(s"Unknown Property Default Value :${c.value.noSpaces}", c.history))
    )
  }
}
