// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1

import io.circe.Json

final case class DataModelProperty(
    `type`: String,
    nullable: Boolean = true,
    targetModelExternalId: Option[String] = None
)

final case class DataModelPropertyIndex(
    indexName: Option[String] = None,
    fields: Option[Seq[String]] = None
)

final case class DataModel(
    externalId: String,
    properties: Option[Map[String, DataModelProperty]] = None,
    `extends`: Option[Seq[String]] = None,
    indexes: Option[Seq[DataModelPropertyIndex]] = None
)

final case class DataModelListInput(includeInheritedProperties: Boolean)

final case class DataModelGetByExternalIdsInput[A](
    items: Seq[A],
    includeInheritedProperties: Boolean,
    ignoreUnknownIds: Boolean
)

sealed trait DataModelInstanceFilter

sealed trait DMIBoolFilter extends DataModelInstanceFilter
final case class DMIAndFilter(and: Seq[DataModelInstanceFilter]) extends DMIBoolFilter
final case class DMIOrFilter(or: Seq[DataModelInstanceFilter]) extends DMIBoolFilter
final case class DMINotFilter(not: DataModelInstanceFilter) extends DMIBoolFilter

sealed trait DMILeafFilter extends DataModelInstanceFilter
final case class DMIEqualsFilter(property: Seq[String], value: Json) extends DMILeafFilter
final case class DMIInFilter(property: Seq[String], values: Seq[Json]) extends DMILeafFilter
final case class DMIRangeFilter(
    property: Seq[String],
    gte: Option[Json] = None,
    gt: Option[Json] = None,
    lte: Option[Json] = None,
    lt: Option[Json] = None
) extends DMILeafFilter {
  require(
    !(gte.isDefined && gt.isDefined) && // can't have both upper bound in the same time
      !(lte.isDefined && lt.isDefined) && // can't have both lower bound in the same time
      (gte.isDefined || gt.isDefined || lte.isDefined || lt.isDefined) // at least one bound must be defined
  )
}
final case class DMIPrefixFilter(property: Seq[String], value: Json) extends DMILeafFilter
final case class DMIExistsFilter(property: Seq[String]) extends DMILeafFilter
final case class DMIContainsAnyFilter(property: Seq[String], values: Seq[Json])
    extends DMILeafFilter
final case class DMIContainsAllFilter(property: Seq[String], values: Seq[Json])
    extends DMILeafFilter

final case class DataModelInstanceQuery(
    modelExternalId: String,
    filter: Option[DataModelInstanceFilter] = None,
    sort: Option[Seq[String]] = None,
    limit: Option[Int] = None,
    cursor: Option[String] = None
)

sealed trait PropertyType

sealed trait PropertyTypePrimitive extends PropertyType
final case class BooleanProperty(value: Boolean) extends PropertyTypePrimitive
/*object BooleanProperty {
  def fromJson(json: Json): Option[BooleanProperty] =
    json.asBoolean.map(BooleanProperty(_))
}*/
final case class NumberProperty(value: Double) extends PropertyTypePrimitive
final case class StringProperty(value: String) extends PropertyTypePrimitive

final case class ArrayProperty[A <: PropertyTypePrimitive](values: Vector[A]) extends PropertyType

final case class DataModelInstance(
    modelExternalId: String,
    properties: Option[Map[String, Json]] = None
)

final case class DataModelInstanceQueryResponse(
    modelExternalId: String,
    properties: Option[Map[String, Json]] = None
)

final case class DataModelInstanceByExternalId(
    externalId: String,
    modelExternalId: String
)