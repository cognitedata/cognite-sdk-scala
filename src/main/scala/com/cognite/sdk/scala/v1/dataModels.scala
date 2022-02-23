// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1

import com.cognite.sdk.scala.common._
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

//final case class DataModelGet(name: String)

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
case class DMIAndFilter(and: Json) extends DMIBoolFilter
case class DMIOrFilter(or: Json) extends DMIBoolFilter
case class DMINotFilter(not: Json) extends DMIBoolFilter

sealed trait DMILeafFilter extends DataModelInstanceFilter
case class DMIInFilter(property: Seq[String], values: Seq[Json]) extends DMILeafFilter

final case class DataModelInstanceQuery(
    model: String,
    filter: Option[DataModelInstanceFilter] = None,
    sort: Option[Seq[String]] = None,
    limit: Option[Int] = None,
    cursor: Option[String] = None
)

final case class DataModelInstanceQueryResponse(
    model: Option[String] = None,
    externalId: Option[String] = None,
    properties: Option[Map[String, String]] = None,
    nextCursor: Option[String] = None
) extends ResponseWithCursor
