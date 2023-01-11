package com.cognite.sdk.scala.v1.fdm.common.filters

import io.circe._

sealed abstract class FilterValueDefinition extends Product with Serializable

object FilterValueDefinition {
  sealed trait ComparableFilterValue extends FilterValueDefinition
  sealed trait LogicalFilterValue extends FilterValueDefinition
  sealed trait SeqFilterValue
      extends FilterValueDefinition // TODO: Assert to void creation with empty lists?

  final case class String(value: java.lang.String) extends ComparableFilterValue
  final case class Double(value: scala.Double) extends ComparableFilterValue
  final case class Integer(value: scala.Long) extends ComparableFilterValue

  final case class Object(value: Json) extends FilterValueDefinition

  final case class StringList(value: Seq[java.lang.String]) extends SeqFilterValue
  final case class DoubleList(value: Seq[scala.Double]) extends SeqFilterValue
  final case class IntegerList(value: Seq[scala.Long]) extends SeqFilterValue
  final case class ObjectList(value: Seq[Json]) extends SeqFilterValue
  final case class BooleanList(value: Seq[scala.Boolean]) extends SeqFilterValue

  final case class Boolean(value: scala.Boolean) extends LogicalFilterValue

  implicit val comparableFilterValueEncoder: Encoder[ComparableFilterValue] =
    Encoder.instance[ComparableFilterValue] {
      case FilterValueDefinition.String(value) => Json.fromString(value)
      case FilterValueDefinition.Double(value) => Json.fromDoubleOrString(value)
      case FilterValueDefinition.Integer(value) => Json.fromLong(value)
    }

  implicit val seqFilterValueEncoder: Encoder[SeqFilterValue] =
    Encoder.instance[SeqFilterValue] {
      case FilterValueDefinition.StringList(value) => Json.fromValues(value.map(Json.fromString))
      case FilterValueDefinition.DoubleList(value) =>
        Json.fromValues(value.map(Json.fromDoubleOrString))
      case FilterValueDefinition.IntegerList(value) => Json.fromValues(value.map(Json.fromLong))
      case FilterValueDefinition.ObjectList(value) => Json.fromValues(value)
      case FilterValueDefinition.BooleanList(value) => Json.fromValues(value.map(Json.fromBoolean))
    }

  implicit val logicalFilterValueEncoder: Encoder[LogicalFilterValue] =
    Encoder.instance[LogicalFilterValue] { case FilterValueDefinition.Boolean(value) =>
      Json.fromBoolean(value)
    }

  implicit val filterValueDefinitionEncoder: Encoder[FilterValueDefinition] =
    Encoder.instance[FilterValueDefinition] {
      case FilterValueDefinition.Object(value) => value
      case v: FilterValueDefinition.ComparableFilterValue => comparableFilterValueEncoder.apply(v)
      case v: FilterValueDefinition.SeqFilterValue => seqFilterValueEncoder.apply(v)
      case v: FilterValueDefinition.LogicalFilterValue => logicalFilterValueEncoder.apply(v)
    }

  implicit val filterValueDefinitionDecoder: Decoder[FilterValueDefinition] = { (c: HCursor) =>
    val result = c.value match {
      case v if v.isString =>
        v.asString
          .map(s => Right[DecodingFailure, FilterValueDefinition](FilterValueDefinition.String(s)))
      case v if v.isObject =>
        v.asObject.map(j =>
          Right[DecodingFailure, FilterValueDefinition](
            FilterValueDefinition.Object(Json.fromJsonObject(j))
          )
        )
      case v if v.isBoolean =>
        v.asBoolean.map(j =>
          Right[DecodingFailure, LogicalFilterValue](FilterValueDefinition.Boolean(j))
        )
      case v if v.isNumber =>
        val numericFilterValue = v.asNumber
          .flatMap(_.toBigDecimal)
          .map(bd =>
            if (bd.isValidLong) {
              FilterValueDefinition.Integer(bd.longValue)
            } else {
              FilterValueDefinition.Double(bd.doubleValue)
            }
          )
        numericFilterValue.map(Right[DecodingFailure, FilterValueDefinition])
      case v if v.isArray =>
        v.asArray
          .flatMap { arr =>
            arr.headOption match {
              case Some(json) if json.isBoolean =>
                Decoder[Seq[scala.Boolean]]
                  .decodeJson(v)
                  .toOption
                  .map(FilterValueDefinition.BooleanList.apply)
              case Some(json) if json.isObject =>
                Some(FilterValueDefinition.ObjectList(arr))
              case Some(json) if json.isString =>
                Decoder[Seq[java.lang.String]]
                  .decodeJson(v)
                  .toOption
                  .map(FilterValueDefinition.StringList.apply)
              case Some(json) if json.isNumber =>
                Decoder[Seq[scala.Long]]
                  .decodeJson(v)
                  .toOption
                  .map(FilterValueDefinition.IntegerList.apply)
                  .orElse(
                    Decoder[Seq[scala.Double]]
                      .decodeJson(v)
                      .toOption
                      .map(FilterValueDefinition.DoubleList.apply)
                  )
              case _ => Some(FilterValueDefinition.ObjectList(Seq.empty))
            }
          }
          .map(Right[DecodingFailure, FilterValueDefinition])
      case o =>
        Some(
          Left(
            DecodingFailure(
              s"Expecting a FilterValueDefinition but found: ${o.noSpaces}",
              c.history
            )
          )
        )
    }
    result.getOrElse(
      Left(
        DecodingFailure(
          s"Expecting a FilterValueDefinition but found: ${c.value.noSpaces}",
          c.history
        )
      )
    )
  }

  implicit val comparableFilterValueDefinitionDecoder: Decoder[ComparableFilterValue] =
    (c: HCursor) =>
      filterValueDefinitionDecoder.apply(c) match {
        case Right(v: ComparableFilterValue) => Right(v)
        case Right(v) =>
          Left(
            DecodingFailure(
              s"Expecting a ComparableFilterValue, but found: ${v.getClass.getSimpleName}",
              c.history
            )
          )
        case Left(err) => Left(err)
      }

  implicit val seqFilterValueDefinitionDecoder: Decoder[SeqFilterValue] =
    (c: HCursor) =>
      filterValueDefinitionDecoder.apply(c) match {
        case Right(v: SeqFilterValue) => Right(v)
        case Right(v) =>
          Left(
            DecodingFailure(
              s"Expecting a SeqFilterValue, but found: ${v.getClass.getSimpleName}",
              c.history
            )
          )
        case Left(err) => Left(err)
      }

  implicit val logicalFilterValueDecoder: Decoder[LogicalFilterValue] =
    (c: HCursor) =>
      filterValueDefinitionDecoder.apply(c) match {
        case Right(v: LogicalFilterValue) => Right(v)
        case Right(v) =>
          Left(
            DecodingFailure(
              s"Expecting a LogicalFilterValue, but found: ${v.getClass.getSimpleName}",
              c.history
            )
          )
        case Left(err) => Left(err)
      }
}
