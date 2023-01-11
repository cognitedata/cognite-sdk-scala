package com.cognite.sdk.scala.v1.fdm.common.filters

import com.cognite.sdk.scala.v1.fdm.common.filters.FilterValueDefinition.{
  ComparableFilterValue,
  SeqFilterValue
}
import com.cognite.sdk.scala.v1.fdm.common.refs.SourceReference
import io.circe.Decoder.Result
import io.circe._
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.syntax._

import java.util.Locale

sealed abstract class FilterDefinition extends Product with Serializable

object FilterDefinition {
  sealed abstract class BoolFilter extends FilterDefinition
  sealed abstract class LeafFilter extends FilterDefinition
  sealed abstract class RangeFilter extends FilterDefinition

  final case class And(filters: Seq[FilterDefinition]) extends BoolFilter
  final case class Or(filters: Seq[FilterDefinition]) extends BoolFilter
  final case class Not(filter: FilterDefinition) extends BoolFilter

  final case class Gte(value: ComparableFilterValue) extends RangeFilter
  final case class Gt(value: ComparableFilterValue) extends RangeFilter
  final case class Lte(value: ComparableFilterValue) extends RangeFilter
  final case class Lt(value: ComparableFilterValue) extends RangeFilter

  final case class Equals(property: Seq[String], value: FilterValueDefinition) extends LeafFilter
  final case class In(property: Seq[String], values: SeqFilterValue) extends LeafFilter
  final case class Range(
      property: Seq[String],
      gte: Option[ComparableFilterValue] = None,
      gt: Option[ComparableFilterValue] = None,
      lte: Option[ComparableFilterValue] = None,
      lt: Option[ComparableFilterValue] = None
  ) extends LeafFilter {
    require(
      !(gte.isDefined && gt.isDefined) && // can't have both upper bound in the same time
        !(lte.isDefined && lt.isDefined) && // can't have both lower bound in the same time
        (gte.isDefined || gt.isDefined || lte.isDefined || lt.isDefined), // at least one bound must be defined
      "Invalid combination of filters. Filters cannot overlap and at least one filter should be defined"
    )
  }
  final case class Prefix(property: Seq[String], value: FilterValueDefinition) extends LeafFilter
  final case class Exists(property: Seq[String]) extends LeafFilter
  final case class ContainsAny(property: Seq[String], values: SeqFilterValue) extends LeafFilter
  final case class ContainsAll(property: Seq[String], values: SeqFilterValue) extends LeafFilter
  final case class Nested(scope: Seq[String], filter: FilterDefinition) extends LeafFilter
  final case class Overlaps(
      startProperty: Seq[String],
      endProperty: Seq[String],
      gte: Option[Gte] = None,
      gt: Option[Gt] = None,
      lte: Option[Lte] = None,
      lt: Option[Lt] = None
  ) extends LeafFilter
  final case class HasData(refs: Seq[SourceReference]) extends LeafFilter

  implicit val andFilterEncoder: Encoder[FilterDefinition.And] =
    Encoder.instance[FilterDefinition.And] { e =>
      Json.fromJsonObject(
        JsonObject(
          e.productPrefix.toLowerCase(Locale.US) -> e.filters.asJson
        )
      )
    }
  implicit val orFilterEncoder: Encoder[FilterDefinition.Or] =
    Encoder.instance[FilterDefinition.Or] { e =>
      Json.fromJsonObject(
        JsonObject(
          e.productPrefix.toLowerCase(Locale.US) -> e.filters.asJson
        )
      )
    }
  implicit val notFilterEncoder: Encoder[FilterDefinition.Not] =
    Encoder.instance[FilterDefinition.Not] { e =>
      Json.fromJsonObject(
        JsonObject(
          e.productPrefix.toLowerCase(Locale.US) -> e.filter.asJson
        )
      )
    }
  implicit val equalsFilterEncoder: Encoder[FilterDefinition.Equals] = deriveEncoder
  implicit val inFilterEncoder: Encoder[FilterDefinition.In] = deriveEncoder
  implicit val rangeFilterEncoder: Encoder[FilterDefinition.Range] =
    deriveEncoder[FilterDefinition.Range].mapJson(_.dropNullValues)
  implicit val prefixFilterEncoder: Encoder[FilterDefinition.Prefix] = deriveEncoder
  implicit val existsFilterEncoder: Encoder[FilterDefinition.Exists] = deriveEncoder
  implicit val containsAnyFilterEncoder: Encoder[FilterDefinition.ContainsAny] = deriveEncoder
  implicit val containsAllFilterEncoder: Encoder[FilterDefinition.ContainsAll] = deriveEncoder
  implicit val nestedFilterEncoder: Encoder[FilterDefinition.Nested] = deriveEncoder
  implicit val overlapsFilterEncoder: Encoder[FilterDefinition.Overlaps] =
    deriveEncoder[FilterDefinition.Overlaps].mapJson(_.dropNullValues)
  implicit val hasDataFilterEncoder: Encoder[FilterDefinition.HasData] =
    Encoder.instance[FilterDefinition.HasData](_.refs.asJson)
  implicit val gteFilterEncoder: Encoder[FilterDefinition.Gte] =
    Encoder.instance[FilterDefinition.Gte](_.value.asJson)
  implicit val gtDataFilterEncoder: Encoder[FilterDefinition.Gt] =
    Encoder.instance[FilterDefinition.Gt](_.value.asJson)
  implicit val lteFilterEncoder: Encoder[FilterDefinition.Lte] =
    Encoder.instance[FilterDefinition.Lte](_.value.asJson)
  implicit val ltFilterEncoder: Encoder[FilterDefinition.Lt] =
    Encoder.instance[FilterDefinition.Lt](_.value.asJson)

  implicit val filterDefinitionEncoder: Encoder[FilterDefinition] =
    Encoder.instance[FilterDefinition] {
      case e: FilterDefinition.And => e.asJson
      case e: FilterDefinition.Or => e.asJson
      case e: FilterDefinition.Not => e.asJson
      case e: FilterDefinition.Equals =>
        Json.fromJsonObject(
          JsonObject(
            e.productPrefix.toLowerCase(Locale.US) -> e.asJson
          )
        )
      case e: FilterDefinition.In =>
        Json.fromJsonObject(
          JsonObject(
            e.productPrefix.toLowerCase(Locale.US) -> e.asJson
          )
        )
      case e: FilterDefinition.Range =>
        Json.fromJsonObject(
          JsonObject(
            e.productPrefix.toLowerCase(Locale.US) -> e.asJson
          )
        )
      case e: FilterDefinition.Prefix =>
        Json.fromJsonObject(
          JsonObject(
            e.productPrefix.toLowerCase(Locale.US) -> e.asJson
          )
        )
      case e: FilterDefinition.Exists =>
        Json.fromJsonObject(
          JsonObject(
            e.productPrefix.toLowerCase(Locale.US) -> e.asJson
          )
        )
      case e: FilterDefinition.ContainsAny =>
        Json.fromJsonObject(
          JsonObject(
            "containsAny" -> e.asJson
          )
        )
      case e: FilterDefinition.ContainsAll =>
        Json.fromJsonObject(
          JsonObject(
            "containsAll" -> e.asJson
          )
        )
      case e: FilterDefinition.Nested =>
        Json.fromJsonObject(
          JsonObject(
            e.productPrefix.toLowerCase(Locale.US) -> e.asJson
          )
        )
      case e: FilterDefinition.Overlaps =>
        Json.fromJsonObject(
          JsonObject(
            e.productPrefix.toLowerCase(Locale.US) -> e.asJson
          )
        )
      case e: FilterDefinition.HasData =>
        Json.fromJsonObject(
          JsonObject(
            "hasData" -> e.refs.asJson
          )
        )
      case e: FilterDefinition.Gte =>
        Json.fromJsonObject(
          JsonObject(
            e.productPrefix.toLowerCase(Locale.US) -> e.asJson
          )
        )
      case e: FilterDefinition.Gt =>
        Json.fromJsonObject(
          JsonObject(
            e.productPrefix.toLowerCase(Locale.US) -> e.asJson
          )
        )
      case e: FilterDefinition.Lte =>
        Json.fromJsonObject(
          JsonObject(
            e.productPrefix.toLowerCase(Locale.US) -> e.asJson
          )
        )
      case e: FilterDefinition.Lt =>
        Json.fromJsonObject(
          JsonObject(
            e.productPrefix.toLowerCase(Locale.US) -> e.asJson
          )
        )
    }

  implicit val andFilterDecoder: Decoder[FilterDefinition.And] = deriveDecoder
  implicit val orFilterDecoder: Decoder[FilterDefinition.Or] = deriveDecoder
  implicit val notFilterDecoder: Decoder[FilterDefinition.Not] = deriveDecoder

  implicit val equalsFilterDecoder: Decoder[FilterDefinition.Equals] = deriveDecoder
  implicit val inFilterDecoder: Decoder[FilterDefinition.In] = deriveDecoder
  implicit val rangeFilterDecoder: Decoder[FilterDefinition.Range] = deriveDecoder
  implicit val prefixFilterDecoder: Decoder[FilterDefinition.Prefix] = deriveDecoder
  implicit val existsFilterDecoder: Decoder[FilterDefinition.Exists] = deriveDecoder
  implicit val containsAnyFilterDecoder: Decoder[FilterDefinition.ContainsAny] = deriveDecoder
  implicit val containsAllFilterDecoder: Decoder[FilterDefinition.ContainsAll] = deriveDecoder
  implicit val nestedFilterDecoder: Decoder[FilterDefinition.Nested] = deriveDecoder
  implicit val overlapsFilterDecoder: Decoder[FilterDefinition.Overlaps] = deriveDecoder
  implicit val hasDataFilterDecoder: Decoder[FilterDefinition.HasData] = deriveDecoder

  implicit val gteFilterDecoder: Decoder[FilterDefinition.Gte] = deriveDecoder
  implicit val gtDataFilterDecoder: Decoder[FilterDefinition.Gt] = deriveDecoder
  implicit val lteFilterDecoder: Decoder[FilterDefinition.Lte] = deriveDecoder
  implicit val ltFilterDecoder: Decoder[FilterDefinition.Lt] = deriveDecoder

  implicit val filterDefinitionDecoder: Decoder[FilterDefinition] = { (c: HCursor) =>
    val and = c.get[FilterDefinition.And]("and")
    val or = c.get[FilterDefinition.Or]("or")
    val not = c.get[FilterDefinition.Or]("not")

    val equals = c.get[FilterDefinition.Equals]("equals")
    val in = c.get[FilterDefinition.In]("in")
    val range = c.get[FilterDefinition.Range]("range")
    val prefix = c.get[FilterDefinition.Prefix]("prefix")
    val exists = c.get[FilterDefinition.Exists]("exists")
    val containsAny = c.get[FilterDefinition.ContainsAny]("containsAny")
    val containsAll = c.get[FilterDefinition.ContainsAll]("containsAll")
    val nested = c.get[FilterDefinition.Nested]("nested")
    val overlaps = c.get[FilterDefinition.Overlaps]("overlaps")
    val hasData = c.get[FilterDefinition.HasData]("hasData")

    val gte = c.get[FilterDefinition.Gte]("gte")
    val gt = c.get[FilterDefinition.Gt]("gt")
    val lte = c.get[FilterDefinition.Lte]("lte")
    val lt = c.get[FilterDefinition.Lt]("lt")

    val result: Result[FilterDefinition] = Seq(
      and,
      or,
      not,
      equals,
      in,
      range,
      prefix,
      exists,
      containsAny,
      containsAll,
      nested,
      overlaps,
      hasData,
      gte,
      gt,
      lte,
      lt
    ).find(_.isRight) match {
      case Some(value) => value
      case None =>
        Left(DecodingFailure(s"Unknown Filter Definition :${c.value.noSpaces}", c.history))
    }
    result
  }
}
