// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1.instances

import io.circe.{Decoder, DecodingFailure, Encoder, HCursor, Json}

sealed abstract class InstancePropertyType extends Product with Serializable

object InstancePropertyType {
  final case class String(value: java.lang.String) extends InstancePropertyType

  final case class Integer(value: scala.Long) extends InstancePropertyType

  final case class Double(value: scala.Double) extends InstancePropertyType

  final case class Boolean(value: scala.Boolean) extends InstancePropertyType

  final case class Object(value: Json) extends InstancePropertyType

  final case class StringList(value: Seq[java.lang.String]) extends InstancePropertyType

  final case class BooleanList(value: Seq[scala.Boolean]) extends InstancePropertyType

  final case class IntegerList(value: Seq[scala.Long]) extends InstancePropertyType

  final case class DoubleList(value: Seq[scala.Double]) extends InstancePropertyType

  final case class ObjectsList(value: Seq[Json]) extends InstancePropertyType

  implicit val instancePropertyTypeDecoder: Decoder[InstancePropertyType] = { (c: HCursor) =>
    val result = c.value match {
      case v if v.isString => v.asString.map(s => Right(InstancePropertyType.String(s)))
      case v if v.isNumber =>
        val numericInstantPropType = v.asNumber.map { jn =>
          jn.toLong match {
            case Some(l) => InstancePropertyType.Integer(l)
            case None => InstancePropertyType.Double(jn.toDouble)
          }
        }
        numericInstantPropType.map(Right(_))
      case v if v.isBoolean => v.asBoolean.map(s => Right(InstancePropertyType.Boolean(s)))
      case v if v.isObject => v.asObject.map(_ => Right(InstancePropertyType.Object(v)))
      case v if v.isArray =>
        val objArrays = v.asArray match {
          case Some(arr) =>
            arr.headOption.map {
              case element if element.isString =>
                Right[DecodingFailure, InstancePropertyType](
                  InstancePropertyType.StringList(arr.flatMap(_.asString))
                )
              case element if element.isBoolean =>
                Right[DecodingFailure, InstancePropertyType](
                  InstancePropertyType.BooleanList(arr.flatMap(_.asBoolean))
                )
              case element if element.isNumber =>
                val matchingPropType = element.asNumber.flatMap(_.toLong) match {
                  case Some(_) =>
                    InstancePropertyType.IntegerList(arr.flatMap(_.asNumber).flatMap(_.toLong))
                  case None =>
                    InstancePropertyType.DoubleList(arr.flatMap(_.asNumber).map(_.toDouble))
                }
                Right[DecodingFailure, InstancePropertyType](matchingPropType)
              case element if element.isObject =>
                Right[DecodingFailure, InstancePropertyType](InstancePropertyType.ObjectsList(arr))
              case _ =>
                Right[DecodingFailure, InstancePropertyType](InstancePropertyType.ObjectsList(arr))
            }
          case None =>
            Some(
              Right[DecodingFailure, InstancePropertyType](
                InstancePropertyType.ObjectsList(Seq.empty[Json])
              )
            )
        }
        objArrays
      case other =>
        Some(
          Left[DecodingFailure, InstancePropertyType](
            DecodingFailure(s"Unknown Instance Property Type: ${other.noSpaces}", c.history)
          )
        )
    }
    result.getOrElse(Left(DecodingFailure(s"Missing Instance Property Type", c.history)))
  }

  implicit val instancePropertyTypeEncoder: Encoder[InstancePropertyType] =
    Encoder.instance[InstancePropertyType] {
      case String(value) => Json.fromString(value)
      case Integer(value) => Json.fromLong(value)
      case Double(value) => Json.fromDoubleOrString(value)
      case Boolean(value) => Json.fromBoolean(value)
      case Object(value) => value
      case StringList(values) => Json.arr(values = values.map(Json.fromString): _*)
      case BooleanList(values) => Json.arr(values = values.map(Json.fromBoolean): _*)
      case IntegerList(values) => Json.arr(values = values.map(Json.fromLong): _*)
      case DoubleList(values) => Json.arr(values = values.map(Json.fromDoubleOrString): _*)
      case ObjectsList(values) => Json.arr(values = values: _*)
    }
}
