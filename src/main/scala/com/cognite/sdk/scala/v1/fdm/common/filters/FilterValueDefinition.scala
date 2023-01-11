package com.cognite.sdk.scala.v1.fdm.common.filters

import cats.implicits._
import io.circe._

sealed abstract class FilterValueDefinition extends Product with Serializable

object FilterValueDefinition {
  sealed abstract class ComparableFilterValue extends FilterValueDefinition
  sealed abstract class LogicalFilterValue extends FilterValueDefinition

  final case class String(value: java.lang.String) extends ComparableFilterValue
  final case class Double(value: scala.Double) extends ComparableFilterValue
  final case class Integer(value: scala.Long) extends ComparableFilterValue
  final case class Object(value: Json) extends ComparableFilterValue
  final case class StringList(value: Seq[java.lang.String]) extends ComparableFilterValue {
    // TODO: Avoid creation with empty lists?
  }
  final case class DoubleList(value: Seq[scala.Double]) extends ComparableFilterValue
  final case class IntegerList(value: Seq[scala.Long]) extends ComparableFilterValue
  final case class ObjectList(value: Seq[Json]) extends ComparableFilterValue
  final case class BooleanList(value: Seq[scala.Boolean]) extends ComparableFilterValue

  final case class Boolean(value: scala.Boolean) extends LogicalFilterValue

  implicit val comparableFilterValueEncoder: Encoder[ComparableFilterValue] =
    Encoder.instance[ComparableFilterValue] {
      case FilterValueDefinition.String(value) => Json.fromString(value)
      case FilterValueDefinition.Double(value) => Json.fromDoubleOrString(value)
      case FilterValueDefinition.Integer(value) => Json.fromLong(value)
      case FilterValueDefinition.Object(value) => value
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
      case v: FilterValueDefinition.ComparableFilterValue => comparableFilterValueEncoder.apply(v)
      case v: FilterValueDefinition.LogicalFilterValue => logicalFilterValueEncoder.apply(v)
    }

  implicit val comparableFilterValueDecoder: Decoder[ComparableFilterValue] = { (c: HCursor) =>
    val result = c.value match {
      case v if v.isString =>
        v.asString
          .map(s => Right[DecodingFailure, ComparableFilterValue](FilterValueDefinition.String(s)))
      case v if v.isObject =>
        v.asObject.map(j =>
          Right[DecodingFailure, ComparableFilterValue](
            FilterValueDefinition.Object(Json.fromJsonObject(j))
          )
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
        numericFilterValue.map(Right[DecodingFailure, ComparableFilterValue])
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
          .map(Right[DecodingFailure, ComparableFilterValue])
      case o =>
        Some(Left(DecodingFailure(s"Unknown Filter Value Definition :${o.noSpaces}", c.history)))
    }
    result.getOrElse(
      Left(DecodingFailure(s"Unknown Filter Value Definition :${c.value.noSpaces}", c.history))
    )
  }

  implicit val logicalFilterValueDecoder: Decoder[LogicalFilterValue] = { (c: HCursor) =>
    val result = c.value match {
      case v if v.isBoolean =>
        v.asBoolean.map(b =>
          Right[DecodingFailure, LogicalFilterValue](FilterValueDefinition.Boolean(b))
        )
      case o =>
        Some(Left(DecodingFailure(s"Unknown Filter Value Definition :${o.noSpaces}", c.history)))
    }
    result.getOrElse(
      Left(DecodingFailure(s"Unknown Filter Value Definition :${c.value.noSpaces}", c.history))
    )
  }

  implicit val filterValueDefinitionDecoder: Decoder[FilterValueDefinition] =
    List[Decoder[FilterValueDefinition]](
      comparableFilterValueDecoder.widen,
      logicalFilterValueDecoder.widen
    ).reduceLeftOption(_ or _)
      .getOrElse(
        Decoder.failed[FilterValueDefinition](
          DecodingFailure("Unable to decode filter value definition", List.empty)
        )
      )
}
