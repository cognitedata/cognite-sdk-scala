package com.cognite.sdk.scala.v1.fdm.common.filters

import com.cognite.sdk.scala.v1.fdm.common.filters.FilterValueDefinition.{
  ComparableFilterValue,
  SeqFilterValue
}
import com.cognite.sdk.scala.v1.fdm.common.sources.SourceReference
import io.circe.Decoder.Result
import io.circe._
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.syntax._

import java.util.Locale

sealed abstract class FilterDefinition extends Product with Serializable

object FilterDefinition {
  sealed abstract class BoolFilter extends FilterDefinition
  sealed abstract class LeafFilter extends FilterDefinition

  final case class And(filters: Seq[FilterDefinition]) extends BoolFilter
  final case class Or(filters: Seq[FilterDefinition]) extends BoolFilter
  final case class Not(filter: FilterDefinition) extends BoolFilter

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
      gte: Option[ComparableFilterValue] = None,
      gt: Option[ComparableFilterValue] = None,
      lte: Option[ComparableFilterValue] = None,
      lt: Option[ComparableFilterValue] = None
  ) extends LeafFilter
  final case class HasData(refs: Seq[SourceReference]) extends LeafFilter
  final case class MatchAll(value: JsonObject) extends LeafFilter

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
  implicit val matchAllFilterEncoder: Encoder[FilterDefinition.MatchAll] = deriveEncoder

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
      case e: FilterDefinition.MatchAll =>
        Json.fromJsonObject(
          JsonObject(
            "matchAll" -> Json.fromJsonObject(e.value)
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
  implicit val matchAllFilterDecoder: Decoder[FilterDefinition.MatchAll] = deriveDecoder

  implicit val filterDefinitionDecoder: Decoder[FilterDefinition] = { (c: HCursor) =>
    val and = c.downField("and").as[FilterDefinition.And]
    val or = c.downField("or").as[FilterDefinition.Or]
    val not = c.downField("not").as[FilterDefinition.Not]

    val equals = c.downField("equals").as[FilterDefinition.Equals]
    val in = c.downField("in").as[FilterDefinition.In]
    val range = c.downField("range").as[FilterDefinition.Range]
    val prefix = c.downField("prefix").as[FilterDefinition.Prefix]
    val exists = c.downField("exists").as[FilterDefinition.Exists]
    val containsAny = c.downField("containsAny").as[FilterDefinition.ContainsAny]
    val containsAll = c.downField("containsAll").as[FilterDefinition.ContainsAll]
    val matchAll = c.downField("matchAll").as[JsonObject].map(MatchAll.apply)
    val nested = c.downField("nested").as[FilterDefinition.Nested]
    val overlaps = c.downField("overlaps").as[FilterDefinition.Overlaps]
    val hasData = c.downField("hasData").as[Seq[SourceReference]].map(HasData.apply)

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
      matchAll
    ).find(_.isRight) match {
      case Some(value) => value
      case None =>
        Left(DecodingFailure(s"Unknown Filter Definition :${c.value.noSpaces}", c.history))
    }
    result
  }
}
