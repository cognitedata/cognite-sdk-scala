package com.cognite.sdk.scala.v1.instances

import com.cognite.sdk.scala.v1.instances.FilterValueDefinition.ComparableFilterValue
import com.cognite.sdk.scala.v1.views.SourceReference
import io.circe._
import io.circe.generic.semiauto.deriveEncoder
import io.circe.syntax._

import java.util.Locale

sealed abstract class FilterDefinition extends Product with Serializable

object FilterDefinition {
  sealed abstract class BoolFilter extends FilterDefinition
  sealed abstract class LeafFilter extends FilterDefinition
  sealed abstract class RangeFilter extends FilterDefinition

  final case class And(filters: Seq[FilterDefinition]) extends BoolFilter
  final case class Or(filters: Seq[FilterDefinition]) extends BoolFilter
  final case class Not(filters: Seq[FilterDefinition]) extends BoolFilter

  final case class Gte(value: ComparableFilterValue) extends RangeFilter
  final case class Gt(value: ComparableFilterValue) extends RangeFilter
  final case class Lte(value: ComparableFilterValue) extends RangeFilter
  final case class Lt(value: ComparableFilterValue) extends RangeFilter

  final case class Equals(property: Seq[String], value: FilterValueDefinition) extends LeafFilter
  final case class In(property: Seq[String], value: Seq[FilterValueDefinition]) extends LeafFilter
  final case class Range(
      property: Seq[String],
      gte: Option[ComparableFilterValue],
      gt: Option[ComparableFilterValue],
      lte: Option[ComparableFilterValue],
      lt: Option[ComparableFilterValue]
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
  final case class ContainsAny(property: Seq[String], value: Seq[FilterValueDefinition])
      extends LeafFilter
  final case class ContainsAll(property: Seq[String], value: Seq[FilterValueDefinition])
      extends LeafFilter
  final case class Nested(scope: Seq[String], filter: FilterDefinition) extends LeafFilter
  final case class Overlaps(
      startProperty: Seq[String],
      endProperty: Seq[String],
      gte: Option[Gte],
      gt: Option[Gt],
      lte: Option[Lte],
      lt: Option[Lt]
  ) extends LeafFilter
  final case class HasData(refs: Seq[SourceReference]) extends LeafFilter

  implicit val andFilterEncoder: Encoder[FilterDefinition.And] = deriveEncoder
  implicit val orFilterEncoder: Encoder[FilterDefinition.Or] = deriveEncoder
  implicit val notFilterEncoder: Encoder[FilterDefinition.Not] = deriveEncoder
  implicit val equalsFilterEncoder: Encoder[FilterDefinition.Equals] = deriveEncoder
  implicit val inFilterEncoder: Encoder[FilterDefinition.In] = deriveEncoder
  implicit val rangeFilterEncoder: Encoder[FilterDefinition.Range] = deriveEncoder
  implicit val prefixFilterEncoder: Encoder[FilterDefinition.Prefix] = deriveEncoder
  implicit val existsFilterEncoder: Encoder[FilterDefinition.Exists] = deriveEncoder
  implicit val containsAnyFilterEncoder: Encoder[FilterDefinition.ContainsAny] = deriveEncoder
  implicit val containsAllFilterEncoder: Encoder[FilterDefinition.ContainsAll] = deriveEncoder
  implicit val nestedFilterEncoder: Encoder[FilterDefinition.Nested] = deriveEncoder
  implicit val overlapsFilterEncoder: Encoder[FilterDefinition.Overlaps] = deriveEncoder
  implicit val hasDataFilterEncoder: Encoder[FilterDefinition.HasData] = deriveEncoder
  implicit val gteFilterEncoder: Encoder[FilterDefinition.Gte] = deriveEncoder
  implicit val gtDataFilterEncoder: Encoder[FilterDefinition.Gt] = deriveEncoder
  implicit val lteFilterEncoder: Encoder[FilterDefinition.Lte] = deriveEncoder
  implicit val ltFilterEncoder: Encoder[FilterDefinition.Lt] = deriveEncoder

  implicit val filterDefinitionEncoder: Encoder[FilterDefinition] =
    Encoder.instance[FilterDefinition] {
      case e: FilterDefinition.And =>
        Json.fromJsonObject(
          JsonObject(
            e.productPrefix.toLowerCase(Locale.US) -> e.asJson
          )
        )
      case e: FilterDefinition.Or =>
        Json.fromJsonObject(
          JsonObject(
            e.productPrefix.toLowerCase(Locale.US) -> e.asJson
          )
        )
      case e: FilterDefinition.Not =>
        Json.fromJsonObject(
          JsonObject(
            e.productPrefix.toLowerCase(Locale.US) -> e.asJson
          )
        )
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
            e.productPrefix.toLowerCase(Locale.US) -> e.asJson
          )
        )
      case e: FilterDefinition.ContainsAll =>
        Json.fromJsonObject(
          JsonObject(
            e.productPrefix.toLowerCase(Locale.US) -> e.asJson
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
            e.productPrefix.toLowerCase(Locale.US) -> e.asJson
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

}
