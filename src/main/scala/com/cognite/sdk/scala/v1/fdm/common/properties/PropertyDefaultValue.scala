// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1.fdm.common.properties

import io.circe._

sealed abstract class PropertyDefaultValue extends Product with Serializable

object PropertyDefaultValue {
  final case class String(value: java.lang.String) extends PropertyDefaultValue
  final case class Int32(value: scala.Int) extends PropertyDefaultValue
  final case class Int64(value: scala.Long) extends PropertyDefaultValue
  final case class Float32(value: scala.Float) extends PropertyDefaultValue
  final case class Float64(value: scala.Double) extends PropertyDefaultValue
  final case class Boolean(value: scala.Boolean) extends PropertyDefaultValue
  final case class Object(value: Json) extends PropertyDefaultValue
  final case class TimeSeriesReference(value: java.lang.String) extends PropertyDefaultValue
  final case class FileReference(value: java.lang.String) extends PropertyDefaultValue
  final case class SequenceReference(value: java.lang.String) extends PropertyDefaultValue

  implicit val propertyDefaultValueEncoder: Encoder[PropertyDefaultValue] =
    Encoder.instance[PropertyDefaultValue] {
      case PropertyDefaultValue.String(value) => Json.fromString(value)
      case PropertyDefaultValue.Int32(value) => Json.fromInt(value)
      case PropertyDefaultValue.Int64(value) => Json.fromLong(value)
      case PropertyDefaultValue.Float32(value) => Json.fromFloatOrString(value)
      case PropertyDefaultValue.Float64(value) => Json.fromDoubleOrString(value)
      case PropertyDefaultValue.Boolean(value) => Json.fromBoolean(value)
      case PropertyDefaultValue.Object(value) => value
      case PropertyDefaultValue.TimeSeriesReference(value) => Json.fromString(value)
      case PropertyDefaultValue.FileReference(value) => Json.fromString(value)
      case PropertyDefaultValue.SequenceReference(value) => Json.fromString(value)
    }

  implicit val propertyDefaultValueDecoder: Decoder[PropertyDefaultValue] = { (c: HCursor) =>
    val result = c.value match {
      case v if v.isString =>
        v.asString.map(s =>
          Right[DecodingFailure, PropertyDefaultValue](PropertyDefaultValue.String(s))
        )
      case v if v.isNumber =>
        val numericPropertyValue = v.asNumber.flatMap { jn =>
          val asString = jn.toString
          val bd = BigDecimal(asString)
          if (asString.contains(".")) {
            if (bd.isDecimalFloat) {
              Some(PropertyDefaultValue.Float32(bd.floatValue))
            } else {
              Some(PropertyDefaultValue.Float64(bd.doubleValue))
            }
          } else {
            if (bd.isValidInt) {
              Some(PropertyDefaultValue.Int32(bd.intValue))
            } else if (bd.isValidLong) {
              Some(PropertyDefaultValue.Int64(bd.longValue))
            } else if (bd.isDecimalFloat) {
              Some(PropertyDefaultValue.Float32(bd.floatValue))
            } else {
              Some(PropertyDefaultValue.Float64(bd.doubleValue))
            }
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
