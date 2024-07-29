// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1.fdm.instances

import com.cognite.sdk.scala.v1.fdm.common.DirectRelationReference
import io.circe._
import io.circe.syntax.EncoderOps

import java.time.format.{DateTimeFormatter, DateTimeFormatterBuilder}
import java.time.temporal.ChronoField.{
  HOUR_OF_DAY,
  MILLI_OF_SECOND,
  MINUTE_OF_HOUR,
  SECOND_OF_MINUTE
}
import java.time.{LocalDate, ZonedDateTime}

sealed abstract class InstancePropertyValue extends Product with Serializable

object InstancePropertyValue {
  final case class String(value: java.lang.String) extends InstancePropertyValue
  final case class Int32(value: scala.Int) extends InstancePropertyValue
  final case class Int64(value: scala.Long) extends InstancePropertyValue
  final case class Float32(value: scala.Float) extends InstancePropertyValue
  final case class Float64(value: scala.Double) extends InstancePropertyValue
  final case class Boolean(value: scala.Boolean) extends InstancePropertyValue
  final case class Date(value: LocalDate) extends InstancePropertyValue

  object Date {
    val formatter: DateTimeFormatter = DateTimeFormatter.ISO_DATE
  }

  final case class Timestamp(value: ZonedDateTime) extends InstancePropertyValue

  object Timestamp {
    // custom formatter to support FDM timestamp format YYYY-MM-DDTHH:MM:SS[.millis][Z|time zone]
    // Millis has to be in 3 digits
    val formatter: DateTimeFormatter = new DateTimeFormatterBuilder().parseCaseInsensitive
      .append(DateTimeFormatter.ISO_LOCAL_DATE)
      .appendLiteral('T')
      .appendValue(HOUR_OF_DAY, 2)
      .appendLiteral(':')
      .appendValue(MINUTE_OF_HOUR, 2)
      .optionalStart
      .appendLiteral(':')
      .appendValue(SECOND_OF_MINUTE, 2)
      .optionalStart
      .appendFraction(MILLI_OF_SECOND, 3, 3, true)
      .optionalStart
      .parseCaseSensitive
      .appendOffsetId()
      .toFormatter()
  }

  final case class Object(value: Json) extends InstancePropertyValue
  final case class TimeSeriesReference(value: java.lang.String) extends InstancePropertyValue
  final case class FileReference(value: java.lang.String) extends InstancePropertyValue
  final case class SequenceReference(value: java.lang.String) extends InstancePropertyValue
  final case class ViewDirectNodeRelation(value: Option[DirectRelationReference])
      extends InstancePropertyValue
  final case class ViewDirectNodeRelationList(value: Seq[DirectRelationReference])
      extends InstancePropertyValue
  final case class StringList(value: Seq[java.lang.String]) extends InstancePropertyValue
  final case class BooleanList(value: Seq[scala.Boolean]) extends InstancePropertyValue
  final case class Int32List(value: Seq[scala.Int]) extends InstancePropertyValue
  final case class Int64List(value: Seq[scala.Long]) extends InstancePropertyValue
  final case class Float32List(value: Seq[scala.Float]) extends InstancePropertyValue
  final case class Float64List(value: Seq[scala.Double]) extends InstancePropertyValue
  final case class DateList(value: Seq[LocalDate]) extends InstancePropertyValue
  final case class TimestampList(value: Seq[ZonedDateTime]) extends InstancePropertyValue
  final case class ObjectList(value: Seq[Json]) extends InstancePropertyValue
  final case class TimeSeriesReferenceList(value: Seq[java.lang.String])
      extends InstancePropertyValue
  final case class FileReferenceList(value: Seq[java.lang.String]) extends InstancePropertyValue
  final case class SequenceReferenceList(value: Seq[java.lang.String]) extends InstancePropertyValue

  implicit val instancePropertyTypeEncoder: Encoder[InstancePropertyValue] =
    Encoder.instance[InstancePropertyValue] {
      case String(value) => Json.fromString(value)
      case Int32(value) => Json.fromInt(value)
      case Int64(value) => Json.fromLong(value)
      case Float32(value) => Json.fromFloatOrString(value)
      case Float64(value) => Json.fromDoubleOrString(value)
      case Boolean(value) => Json.fromBoolean(value)
      case Date(value) => Json.fromString(value.format(InstancePropertyValue.Date.formatter))
      case Timestamp(value) =>
        Json.fromString(value.format(InstancePropertyValue.Timestamp.formatter))
      case ViewDirectNodeRelation(value) => value.map(_.asJson).getOrElse(Json.Null)
      case Object(value) => value
      case TimeSeriesReference(value) => Json.fromString(value)
      case FileReference(value) => Json.fromString(value)
      case SequenceReference(value) => Json.fromString(value)
      case StringList(values) => Json.arr(values = values.map(Json.fromString): _*)
      case BooleanList(values) => Json.arr(values = values.map(Json.fromBoolean): _*)
      case Int32List(values) => Json.arr(values = values.map(Json.fromInt): _*)
      case Int64List(values) => Json.arr(values = values.map(Json.fromLong): _*)
      case Float32List(values) => Json.arr(values = values.map(Json.fromFloatOrString): _*)
      case Float64List(values) => Json.arr(values = values.map(Json.fromDoubleOrString): _*)
      case DateList(values) =>
        Json.arr(values = values.map(d => Json.fromString(d.format(Date.formatter))): _*)
      case TimestampList(values) =>
        Json.arr(values = values.map(d => Json.fromString(d.format(Timestamp.formatter))): _*)
      case ViewDirectNodeRelationList(values) =>
        Json.arr(values = values.map(value => value.asJson): _*)
      case ObjectList(values) => Json.arr(values = values: _*)
      case TimeSeriesReferenceList(values) => Json.arr(values = values.map(Json.fromString): _*)
      case FileReferenceList(values) => Json.arr(values = values.map(Json.fromString): _*)
      case SequenceReferenceList(values) => Json.arr(values = values.map(Json.fromString): _*)
    }
}
