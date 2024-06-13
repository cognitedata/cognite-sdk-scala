// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1.fdm.instances

import cats.implicits.catsSyntaxEq
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
import scala.util.{Success, Try}

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

  implicit val instancePropertyTypeDecoder: Decoder[InstancePropertyValue] = { (c: HCursor) =>
    val result = c.value match {
      case v if v.isString =>
        v.asString.flatMap { s =>
          Try(ZonedDateTime.parse(s))
            .map(InstancePropertyValue.Timestamp.apply)
            .orElse(
              Try(LocalDate.parse(s))
                .map(InstancePropertyValue.Date.apply)
            )
            .orElse(Success(InstancePropertyValue.String(s)))
            .toOption
            .map(Right[DecodingFailure, InstancePropertyValue])
        }
      case v if v.isNumber =>
        val numericInstantPropType = v.asNumber.flatMap { jn =>
          val bd = BigDecimal(jn.toString)
          if (jn.toString.contains(".")) { // 1.0 should be a Double not Long
            if (bd.isExactFloat) {
              Some(InstancePropertyValue.Float32(bd.floatValue))
            } else {
              Some(InstancePropertyValue.Float64(bd.doubleValue))
            }
          } else {
            if (bd.isValidInt) {
              Some(InstancePropertyValue.Int32(bd.intValue))
            } else if (bd.isValidLong) {
              Some(InstancePropertyValue.Int64(bd.longValue))
            } else {
              Some(InstancePropertyValue.Float64(bd.doubleValue))
            }
          }
        }
        numericInstantPropType.map(Right(_))
      case v if v.isBoolean => v.asBoolean.map(s => Right(InstancePropertyValue.Boolean(s)))
      case v if v.isObject && isDirectRelation(v) =>
        Some(decodeDirectRelation(v, c) match {
          case Left(d) => Left(d)
          case Right(d: DirectRelationReference) =>
            Right(InstancePropertyValue.ViewDirectNodeRelation(Some(d)))
        })
      case v if v.isObject =>
        Some(Right(InstancePropertyValue.Object(v)))
      case v if v.isArray =>
        val objArrays = v.asArray match {
          case Some(arr) =>
            arr.headOption.map {
              case element
                  if element.isString && element.asString
                    .flatMap(s =>
                      Try(
                        ZonedDateTime.parse(s)
                      ).toOption
                    )
                    .nonEmpty =>
                Right[DecodingFailure, InstancePropertyValue](
                  InstancePropertyValue.TimestampList(
                    arr
                      .flatMap(_.asString)
                      .flatMap(s =>
                        Try(
                          ZonedDateTime.parse(s)
                        ).toOption
                      )
                  )
                )
              case element
                  if element.isString && element.asString
                    .flatMap(s => Try(LocalDate.parse(s)).toOption)
                    .nonEmpty =>
                Right[DecodingFailure, InstancePropertyValue](
                  InstancePropertyValue.DateList(
                    arr
                      .flatMap(_.asString)
                      .flatMap(s => Try(LocalDate.parse(s)).toOption)
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
              case element if element.isNumber =>
                val matchingPropType = element.asNumber
                  .flatMap(_.toBigDecimal) match {
                  case Some(bd) if bd.isValidInt || bd.isValidLong =>
                    Decoder[Seq[Int]]
                      .decodeJson(v)
                      .toOption
                      .map(InstancePropertyValue.Int32List.apply)
                      .orElse(
                        Decoder[Seq[Long]]
                          .decodeJson(v)
                          .toOption
                          .map(InstancePropertyValue.Int64List.apply)
                      )
                      .getOrElse(
                        InstancePropertyValue.Float64List(
                          arr.flatMap(_.asNumber).map(_.toDouble)
                        )
                      )
                  case _ =>
                    InstancePropertyValue.Float64List(arr.flatMap(_.asNumber).map(_.toDouble))
                }
                Right[DecodingFailure, InstancePropertyValue](matchingPropType)
              case element if element.isObject && isDirectRelation(element) =>
                arr.toList.partitionMap(decodeDirectRelation(_, c)) match {
                  case (Nil, rights) =>
                    Right(InstancePropertyValue.ViewDirectNodeRelationList(rights))
                  case (lefts, _) =>
                    Left(
                      lefts.headOption.getOrElse(
                        DecodingFailure(
                          f"List of direct relations contains elements that can't be serialized into direct relation. Errors: " +
                            f"${lefts.map(_.message).mkString(", ")}",
                          c.history
                        )
                      )
                    )
                }
              case _ =>
                Right[DecodingFailure, InstancePropertyValue](
                  InstancePropertyValue.ObjectList(arr)
                )
            }
          case None =>
            Some(
              Right[DecodingFailure, InstancePropertyValue](
                InstancePropertyValue.ObjectList(Seq.empty[Json])
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

  private def isDirectRelation(v: Json) =
    v.asObject.exists(obj => obj.contains("externalId") && obj.contains("space") && obj.size === 2)
  private def decodeDirectRelation(
      v: Json,
      c: HCursor
  ): Either[DecodingFailure, DirectRelationReference] =
    v.asObject
      .map(obj =>
        for {
          spaceJson <- obj("space").toRight(DecodingFailure("missing space", c.history))
          space <- spaceJson.asString.toRight(DecodingFailure("space isn't string", c.history))
          externalIdJson <- obj("externalId").toRight(
            DecodingFailure("missing externalId", c.history)
          )
          externalId <- externalIdJson.asString.toRight(
            DecodingFailure("externalId isn't string", c.history)
          )
        } yield DirectRelationReference(
          space,
          externalId
        )
      )
      .getOrElse(Left(DecodingFailure("could not deserialize into object", c.history)))

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
        Json.arr(values =
          values.map(d => Json.fromString(d.format(InstancePropertyValue.Date.formatter))): _*
        )
      case TimestampList(values) =>
        Json.arr(values =
          values.map(d => Json.fromString(d.format(InstancePropertyValue.Timestamp.formatter))): _*
        )
      case ViewDirectNodeRelationList(values) =>
        Json.arr(values = values.map(value => value.asJson): _*)
      case ObjectList(values) => Json.arr(values = values: _*)
      case TimeSeriesReferenceList(values) => Json.arr(values = values.map(Json.fromString): _*)
      case FileReferenceList(values) => Json.arr(values = values.map(Json.fromString): _*)
      case SequenceReferenceList(values) => Json.arr(values = values.map(Json.fromString): _*)
    }
}
