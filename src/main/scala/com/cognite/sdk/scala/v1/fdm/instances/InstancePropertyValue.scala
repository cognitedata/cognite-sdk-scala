// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1.fdm.instances

import io.circe._

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, ZonedDateTime}
import scala.util.{Success, Try}

sealed abstract class InstancePropertyValue extends Product with Serializable

object InstancePropertyValue {
  final case class String(value: java.lang.String) extends InstancePropertyValue

  final case class Integer(value: scala.Long) extends InstancePropertyValue

  final case class Double(value: scala.Double) extends InstancePropertyValue

  final case class Boolean(value: scala.Boolean) extends InstancePropertyValue

  final case class Date(value: LocalDate) extends InstancePropertyValue

  final case class Timestamp(value: ZonedDateTime) extends InstancePropertyValue

  final case class Object(value: Json) extends InstancePropertyValue

  final case class StringList(value: Seq[java.lang.String]) extends InstancePropertyValue

  final case class BooleanList(value: Seq[scala.Boolean]) extends InstancePropertyValue

  final case class IntegerList(value: Seq[scala.Long]) extends InstancePropertyValue

  final case class DoubleList(value: Seq[scala.Double]) extends InstancePropertyValue

  final case class DateList(value: Seq[LocalDate]) extends InstancePropertyValue

  final case class TimestampList(value: Seq[ZonedDateTime]) extends InstancePropertyValue

  final case class ObjectsList(value: Seq[Json]) extends InstancePropertyValue

  implicit val instancePropertyTypeDecoder: Decoder[InstancePropertyValue] = { (c: HCursor) =>
    val result = c.value match {
      case v if v.isString =>
        v.asString.flatMap { s =>
          Try(ZonedDateTime.parse(s, DateTimeFormatter.ISO_ZONED_DATE_TIME))
            .map(InstancePropertyValue.Timestamp)
            .orElse(
              Try(LocalDate.parse(s, DateTimeFormatter.ISO_DATE)).map(InstancePropertyValue.Date)
            )
            .orElse(Success(InstancePropertyValue.String(s)))
            .toOption
            .map(Right[DecodingFailure, InstancePropertyValue])
        }
      case v if v.isNumber =>
        val numericInstantPropType = v.asNumber.flatMap { jn =>
          if (jn.toString.contains(".")) {
            Some(InstancePropertyValue.Double(jn.toDouble))
          } else {
            jn.toLong.map(InstancePropertyValue.Integer)
          }
        }
        numericInstantPropType.map(Right(_))
      case v if v.isBoolean => v.asBoolean.map(s => Right(InstancePropertyValue.Boolean(s)))
      case v if v.isObject => v.asObject.map(_ => Right(InstancePropertyValue.Object(v)))
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
                Right[DecodingFailure, InstancePropertyValue](
                  InstancePropertyValue.TimestampList(
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
                Right[DecodingFailure, InstancePropertyValue](
                  InstancePropertyValue.DateList(
                    arr
                      .flatMap(_.asString)
                      .flatMap(s => Try(LocalDate.parse(s, DateTimeFormatter.ISO_DATE)).toOption)
                  )
                )
              case element if element.isString =>
                Right[DecodingFailure, InstancePropertyValue](
                  InstancePropertyValue.StringList(arr.flatMap(_.asString))
                )
              case element if element.isBoolean =>
                Right[DecodingFailure, InstancePropertyValue](
                  InstancePropertyValue.BooleanList(arr.flatMap(_.asBoolean))
                )
              case element if element.isNumber => // 1.0 should be Double not Long
                val matchingPropType = element.asNumber.map(_.toString.contains(".")) match {
                  case Some(true) =>
                    InstancePropertyValue.DoubleList(arr.flatMap(_.asNumber).map(_.toDouble))
                  case _ =>
                    InstancePropertyValue.IntegerList(arr.flatMap(_.asNumber).flatMap(_.toLong))
                }
                Right[DecodingFailure, InstancePropertyValue](matchingPropType)
              case element if element.isObject =>
                Right[DecodingFailure, InstancePropertyValue](
                  InstancePropertyValue.ObjectsList(arr)
                )
              case _ =>
                Right[DecodingFailure, InstancePropertyValue](
                  InstancePropertyValue.ObjectsList(arr)
                )
            }
          case None =>
            Some(
              Right[DecodingFailure, InstancePropertyValue](
                InstancePropertyValue.ObjectsList(Seq.empty[Json])
              )
            )
        }
        objArrays
      case other =>
        Some(
          Left[DecodingFailure, InstancePropertyValue](
            DecodingFailure(s"Unknown Instance Property Type: ${other.noSpaces}", c.history)
          )
        )
    }
    result.getOrElse(Left(DecodingFailure(s"Missing Instance Property Type", c.history)))
  }

  implicit val instancePropertyTypeEncoder: Encoder[InstancePropertyValue] =
    Encoder.instance[InstancePropertyValue] {
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
