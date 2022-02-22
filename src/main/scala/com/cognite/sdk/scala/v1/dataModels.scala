// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1

import com.cognite.sdk.scala.common._

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

final case class DataModelGet(name: String)

final case class DataModelMapping(
    externalId: Option[String] = None,
    properties: Option[Map[String, DataModelProperty]] = None
)

final case class DataModelInstanceCreate(
    model: Option[String] = None,
    externalId: Option[String] = None,
    properties: Option[Map[String, String]] =
      None // TODO Recheck if need to adapt for number and bool
)

/*sealed trait DSLFilter
case class DSLBoolFilter()*/

final case class DataModelInstanceQuery(
    model: String,
    filter: Option[Map[String, String]] = None, // TODO
    sort: Option[Seq[String]] = None,
    limit: Option[Int] = None,
    cursor: Option[String] = None
)

final case class DataModelInstanceQueryResponse(
    model: Option[String] = None,
    externalId: Option[String] = None,
    properties: Option[Map[String, DataModelProperty]] = None,
    nextCursor: Option[String] = None
) extends ResponseWithCursor
