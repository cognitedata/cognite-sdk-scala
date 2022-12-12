package com.cognite.sdk.scala.v1.fdm.common.filters

import cats.implicits._
import io.circe._

sealed abstract class FilterValueDefinition extends Product with Serializable

object FilterValueDefinition {
  sealed abstract class ComparableFilterValue extends FilterValueDefinition
  sealed abstract class LogicalFilterValue extends FilterValueDefinition
  final case class String(value: java.lang.String) extends ComparableFilterValue
  final case class Number(value: scala.Double) extends ComparableFilterValue
  final case class Integer(value: scala.Long) extends ComparableFilterValue
  final case class Boolean(value: scala.Boolean) extends LogicalFilterValue

  implicit val comparableFilterValueEncoder: Encoder[ComparableFilterValue] =
    Encoder.instance[ComparableFilterValue] {
      case FilterValueDefinition.String(value) => Json.fromString(value)
      case FilterValueDefinition.Number(value) => Json.fromDoubleOrString(value)
      case FilterValueDefinition.Integer(value) => Json.fromLong(value)
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
        v.asString.map(s =>
          Right[DecodingFailure, ComparableFilterValue](FilterValueDefinition.String(s))
        )
      case v if v.isNumber =>
        val numericFilterValue = v.asNumber.flatMap(n =>
          if (n.toString.contains(".")) {
            Some(FilterValueDefinition.Number(n.toDouble))
          } else {
            n.toLong.map(FilterValueDefinition.Integer)
          }
        )
        numericFilterValue.map(Right[DecodingFailure, ComparableFilterValue])
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
    ).reduceLeftOption(_ or _).getOrElse(comparableFilterValueDecoder.widen)
}
