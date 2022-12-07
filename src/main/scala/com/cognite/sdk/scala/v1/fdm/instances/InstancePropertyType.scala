// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1.fdm.instances

import io.circe._

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, ZonedDateTime}
import scala.util.{Success, Try}

sealed abstract class InstancePropertyType extends Product with Serializable

object InstancePropertyType {
  final case class String(value: java.lang.String) extends InstancePropertyType

  final case class Integer(value: scala.Long) extends InstancePropertyType

  final case class Double(value: scala.Double) extends InstancePropertyType

  final case class Boolean(value: scala.Boolean) extends InstancePropertyType

  final case class Date(value: LocalDate) extends InstancePropertyType

  final case class Timestamp(value: ZonedDateTime) extends InstancePropertyType

  final case class Object(value: Json) extends InstancePropertyType

  final case class StringList(value: Seq[java.lang.String]) extends InstancePropertyType

  final case class BooleanList(value: Seq[scala.Boolean]) extends InstancePropertyType

  final case class IntegerList(value: Seq[scala.Long]) extends InstancePropertyType

  final case class DoubleList(value: Seq[scala.Double]) extends InstancePropertyType

  final case class DateList(value: Seq[LocalDate]) extends InstancePropertyType

  final case class TimestampList(value: Seq[ZonedDateTime]) extends InstancePropertyType

  final case class ObjectsList(value: Seq[Json]) extends InstancePropertyType

  implicit val instancePropertyTypeDecoder: Decoder[InstancePropertyType] = { (c: HCursor) =>
    val result = c.value match {
      case v if v.isString =>
        v.asString.flatMap { s =>
          Try(ZonedDateTime.parse(s, DateTimeFormatter.ISO_ZONED_DATE_TIME))
            .map(InstancePropertyType.Timestamp)
            .orElse(
              Try(LocalDate.parse(s, DateTimeFormatter.ISO_DATE)).map(InstancePropertyType.Date)
            )
            .orElse(Success(InstancePropertyType.String(s)))
            .toOption
            .map(Right[DecodingFailure, InstancePropertyType])
        }
      case v if v.isNumber =>
        val numericInstantPropType = v.asNumber.flatMap { jn =>
          if (jn.toString.contains(".")) {
            Some(InstancePropertyType.Double(jn.toDouble))
          } else {
            jn.toLong.map(InstancePropertyType.Integer)
          }
        }
        numericInstantPropType.map(Right(_))
      case v if v.isBoolean => v.asBoolean.map(s => Right(InstancePropertyType.Boolean(s)))
      case v if v.isObject => v.asObject.map(_ => Right(InstancePropertyType.Object(v)))
      case v if v.isArray =>
        val objArrays = v.asArray match {
          case Some(arr) =>
            arr.headOption.map {
              case element
                  if element.isString && element.asString
                    .flatMap(s =>
                      Try(ZonedDateTime.parse(s, DateTimeFormatter.ISO_ZONED_DATE_TIME)).toOption
                    )
                    .nonEmpty =>
                Right[DecodingFailure, InstancePropertyType](
                  InstancePropertyType.TimestampList(
                    arr
                      .flatMap(_.asString)
                      .flatMap(s =>
                        Try(
                          ZonedDateTime.parse(s, DateTimeFormatter.ISO_ZONED_DATE_TIME)
                        ).toOption
                      )
                  )
                )
              case element
                  if element.isString && element.asString
                    .flatMap(s => Try(LocalDate.parse(s, DateTimeFormatter.ISO_DATE)).toOption)
                    .nonEmpty =>
                Right[DecodingFailure, InstancePropertyType](
                  InstancePropertyType.DateList(
                    arr
                      .flatMap(_.asString)
                      .flatMap(s => Try(LocalDate.parse(s, DateTimeFormatter.ISO_DATE)).toOption)
                  )
                )
              case element if element.isString =>
                Right[DecodingFailure, InstancePropertyType](
                  InstancePropertyType.StringList(arr.flatMap(_.asString))
                )
              case element if element.isBoolean =>
                Right[DecodingFailure, InstancePropertyType](
                  InstancePropertyType.BooleanList(arr.flatMap(_.asBoolean))
                )
              case element if element.isNumber => // 1.0 should be Double not Long
                val matchingPropType = element.asNumber.map(_.toString.contains(".")) match {
                  case Some(true) =>
                    InstancePropertyType.DoubleList(arr.flatMap(_.asNumber).map(_.toDouble))
                  case _ =>
                    InstancePropertyType.IntegerList(arr.flatMap(_.asNumber).flatMap(_.toLong))
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
      case Date(value) => Json.fromString(value.format(DateTimeFormatter.ISO_DATE))
      case Timestamp(value) => Json.fromString(value.format(DateTimeFormatter.ISO_ZONED_DATE_TIME))
      case Object(value) => value
      // TODO: Handle null values in Array elements
      case StringList(values) => Json.arr(values = values.map(Json.fromString): _*)
      case BooleanList(values) => Json.arr(values = values.map(Json.fromBoolean): _*)
      case IntegerList(values) => Json.arr(values = values.map(Json.fromLong): _*)
      case DoubleList(values) => Json.arr(values = values.map(Json.fromDoubleOrString): _*)
      case DateList(values) =>
        Json.arr(values =
          values.map(d => Json.fromString(d.format(DateTimeFormatter.ISO_DATE))): _*
        )
      case TimestampList(values) =>
        Json.arr(values =
          values.map(d => Json.fromString(d.format(DateTimeFormatter.ISO_ZONED_DATE_TIME))): _*
        )
      case ObjectsList(values) => Json.arr(values = values: _*)
    }
}
