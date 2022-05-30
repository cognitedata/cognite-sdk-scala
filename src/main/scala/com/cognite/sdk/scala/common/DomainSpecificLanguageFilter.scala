// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.common

import com.cognite.sdk.scala.v1._
import io.circe._
import io.circe.generic.semiauto.deriveEncoder
import io.circe.syntax._

sealed trait DomainSpecificLanguageFilter

case object EmptyFilter extends DomainSpecificLanguageFilter

sealed trait DSLBoolFilter extends DomainSpecificLanguageFilter
final case class DSLAndFilter(and: Seq[DomainSpecificLanguageFilter]) extends DSLBoolFilter
final case class DSLOrFilter(or: Seq[DomainSpecificLanguageFilter]) extends DSLBoolFilter
final case class DSLNotFilter(not: DomainSpecificLanguageFilter) extends DSLBoolFilter

sealed trait DSLLeafFilter extends DomainSpecificLanguageFilter
final case class DSLEqualsFilter(property: Seq[String], value: DataModelProperty[_])
    extends DSLLeafFilter
final case class DSLInFilter(property: Seq[String], values: Seq[DataModelProperty[_]])
    extends DSLLeafFilter
final case class DSLRangeFilter(
    property: Seq[String],
    gte: Option[DataModelProperty[_]] = None,
    gt: Option[DataModelProperty[_]] = None,
    lte: Option[DataModelProperty[_]] = None,
    lt: Option[DataModelProperty[_]] = None
) extends DSLLeafFilter {
  require(
    !(gte.isDefined && gt.isDefined) && // can't have both upper bound in the same time
      !(lte.isDefined && lt.isDefined) && // can't have both lower bound in the same time
      (gte.isDefined || gt.isDefined || lte.isDefined || lt.isDefined) // at least one bound must be defined
  )
}
final case class DSLPrefixFilter(property: Seq[String], value: DataModelProperty[_])
    extends DSLLeafFilter
final case class DSLExistsFilter(property: Seq[String]) extends DSLLeafFilter
final case class DSLContainsAnyFilter(property: Seq[String], values: Seq[DataModelProperty[_]])
    extends DSLLeafFilter
final case class DSLContainsAllFilter(property: Seq[String], values: Seq[DataModelProperty[_]])
    extends DSLLeafFilter

object DomainSpecificLanguageFilter {

  // scalastyle:off cyclomatic.complexity
  implicit val propEncoder: Encoder[DataModelProperty[_]] = {
    case b: PropertyType.Boolean.Property => b.value.asJson
    case i: PropertyType.Int.Property => i.value.asJson
    case bi: PropertyType.Bigint.Property => bi.value.asJson
    case f: PropertyType.Float32.Property => f.value.asJson
    case d: PropertyType.Float64.Property => d.value.asJson
    case bd: PropertyType.Numeric.Property => bd.value.asJson
    case s: PropertyType.Text.Property => s.value.asJson
    case j: PropertyType.Json.Property => j.value.asJson
    case ts: PropertyType.Timestamp.Property => ts.value.asJson
    case d: PropertyType.Date.Property => d.value.asJson
    case gm: PropertyType.Geometry.Property => gm.value.asJson
    case gg: PropertyType.Geography.Property => gg.value.asJson
    case dr: PropertyType.DirectRelation.Property => dr.value.asJson
    case b: PropertyType.Array.Boolean.Property => b.value.asJson
    case i: PropertyType.Array.Int.Property => i.value.asJson
    case bi: PropertyType.Array.Bigint.Property => bi.value.asJson
    case f: PropertyType.Array.Float32.Property => f.value.asJson
    case d: PropertyType.Array.Float64.Property => d.value.asJson
    case bd: PropertyType.Array.Numeric.Property => bd.value.asJson
    case s: PropertyType.Array.Text.Property => s.value.asJson
    case j: PropertyType.Array.Json.Property => j.value.asJson
    case ts: PropertyType.Array.Timestamp.Property => ts.value.asJson
    case d: PropertyType.Array.Date.Property => d.value.asJson
    case gm: PropertyType.Array.Geometry.Property => gm.value.asJson
    case gg: PropertyType.Array.Geography.Property => gg.value.asJson
    case _ => throw new Exception("unknown property type")
  }

  implicit val andFilterEncoder: Encoder[DSLAndFilter] = deriveEncoder[DSLAndFilter]
  implicit val orFilterEncoder: Encoder[DSLOrFilter] = deriveEncoder[DSLOrFilter]
  implicit val notFilterEncoder: Encoder[DSLNotFilter] = deriveEncoder[DSLNotFilter]

  implicit val equalsFilterEncoder: Encoder[DSLEqualsFilter] =
    Encoder.forProduct2[DSLEqualsFilter, Seq[String], DataModelProperty[_]]("property", "value")(
      dmiEqF => (dmiEqF.property, dmiEqF.value)
    )
  implicit val inFilterEncoder: Encoder[DSLInFilter] = deriveEncoder[DSLInFilter]
  implicit val rangeFilterEncoder: Encoder[DSLRangeFilter] =
    deriveEncoder[DSLRangeFilter].mapJson(_.dropNullValues) // VH TODO make this common

  implicit val prefixFilterEncoder: Encoder[DSLPrefixFilter] =
    Encoder.forProduct2[DSLPrefixFilter, Seq[String], DataModelProperty[_]]("property", "value")(
      dmiPxF => (dmiPxF.property, dmiPxF.value)
    )
  implicit val existsFilterEncoder: Encoder[DSLExistsFilter] = deriveEncoder[DSLExistsFilter]
  implicit val containsAnyFilterEncoder: Encoder[DSLContainsAnyFilter] =
    deriveEncoder[DSLContainsAnyFilter]
  implicit val containsAllFilterEncoder: Encoder[DSLContainsAllFilter] =
    deriveEncoder[DSLContainsAllFilter]

  implicit val filterEncoder: Encoder[DomainSpecificLanguageFilter] = {
    case EmptyFilter =>
      Json.fromFields(Seq.empty)
    case b: DSLBoolFilter =>
      b match {
        case f: DSLAndFilter => f.asJson
        case f: DSLOrFilter => f.asJson
        case f: DSLNotFilter => f.asJson
      }
    case l: DSLLeafFilter =>
      l match {
        case f: DSLInFilter => Json.obj(("in", f.asJson))
        case f: DSLEqualsFilter => Json.obj(("equals", f.asJson))
        case f: DSLRangeFilter => Json.obj(("range", f.asJson))
        case f: DSLPrefixFilter => Json.obj(("prefix", f.asJson))
        case f: DSLExistsFilter => Json.obj(("exists", f.asJson))
        case f: DSLContainsAnyFilter => Json.obj(("containsAny", f.asJson))
        case f: DSLContainsAllFilter => Json.obj(("containsAll", f.asJson))
      }
  }

}
