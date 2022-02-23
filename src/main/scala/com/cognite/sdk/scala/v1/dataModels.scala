// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1

import io.circe.Json

final case class DataModelProperty(
    `type`: String,
    nullable: Option[Boolean] = Some(true)
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

final case class DataModelMapping(
    externalId: Option[String] = None,
    properties: Option[Map[String, DataModelProperty]] = None
)

final case class DataModelInstance(
    modelExternalId: Option[String] = None,
    externalId: Option[String] = None,
    properties: Option[Map[String, Json]] = None
)

sealed trait DataModelInstanceFilter

sealed trait DMIBoolFilter extends DataModelInstanceFilter
case class DMIAndFilter(and: Seq[DataModelInstanceFilter]) extends DMIBoolFilter
case class DMIOrFilter(or: Seq[DataModelInstanceFilter]) extends DMIBoolFilter
case class DMINotFilter(not: DataModelInstanceFilter) extends DMIBoolFilter

sealed trait DMILeafFilter extends DataModelInstanceFilter
case class DMIEqualsFilter(property: Seq[String], value: Json) extends DMILeafFilter
case class DMIInFilter(property: Seq[String], values: Seq[Json]) extends DMILeafFilter
case class DMIRangeFilter(
    property: Seq[String],
    gte: Option[Json] = None,
    gt: Option[Json] = None,
    lte: Option[Json] = None,
    lt: Option[Json] = None
) extends DMILeafFilter {
  require(
    !(gte.isDefined && gt.isDefined) && // can't have both upper bound in the same time
      !(lte.isDefined && lt.isDefined) && // can't have both lower bound in the same time
      (gte.isDefined || gt.isDefined || lte.isDefined || lt.isDefined) // at least oue bound must be defined
  )
}
case class DMIPrefixFilter(property: Seq[String], value: Json) extends DMILeafFilter
case class DMIExistsFilter(property: Seq[String]) extends DMILeafFilter
case class DMIContainsAnyFilter(property: Seq[String], values: Seq[Json]) extends DMILeafFilter
case class DMIContainsAllFilter(property: Seq[String], values: Seq[Json]) extends DMILeafFilter

final case class DataModelInstanceQuery(
    modelExternalId: String,
    filter: Option[DataModelInstanceFilter] = None,
    sort: Option[Seq[String]] = None,
    limit: Option[Int] = None,
    cursor: Option[String] = None
)

final case class DataModelInstanceQueryResponse(
    modelExternalId: String,
    externalId: String,
    properties: Option[Map[String, Json]] = None
)
