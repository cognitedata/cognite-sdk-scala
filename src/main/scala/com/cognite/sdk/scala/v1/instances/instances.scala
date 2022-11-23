// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1.instances

import com.cognite.sdk.scala.v1.containers.{
  ContainerPropertyType,
  ContainerReference,
  PropertyDefaultValue
}
import com.cognite.sdk.scala.v1.views.{SourceReference, ViewReference}

import java.time.Instant

final case class InstanceReadRequest(
    limit: Option[Int],
    cursor: Option[String],
    space: Option[String],
    includeTypeInfo: Option[Boolean],
    `type`: Option[InstanceType],
    view: Option[ViewReference],
    container: Option[ContainerReference]
)

final case class DirectRelationReference(space: String, externalId: String)

final case class InstanceViewData(
    view: ViewReference,
    properties: Map[String, InstancePropertyType]
)

final case class InstanceContainerData(
    container: ContainerReference,
    properties: Map[String, InstancePropertyType]
)

final case class InstanceCreate(
    items: Seq[InstanceTypeWriteItem],
    autoCreateStartNodes: Option[Boolean],
    autoCreateEndNodes: Option[Boolean],
    replace: Option[Boolean]
)

final case class InstanceCreateResponse(
    `type`: InstanceType,
    space: String,
    externalId: String,
    createdTime: Instant,
    lastUpdatedTime: Instant
)

final case class InstanceRetrieve(
    sources: Seq[SourceReference],
    instanceType: InstanceType,
    externalId: String,
    space: String
)

final case class InstanceRetrieveRequest(items: Seq[InstanceRetrieve], includeTyping: Boolean)

sealed trait InstanceDefinition {
  val `type`: InstanceType
}

final case class NodeDefinition(
    space: String,
    externalId: String,
    createdTime: Option[Instant],
    lastUpdatedTime: Option[Instant],
    properties: Option[Map[String, Map[String, Map[String, InstancePropertyType]]]]
) extends InstanceDefinition {
  override val `type`: InstanceType = InstanceType.Node
}

final case class EdgeDefinition(
    relation: DirectRelationReference,
    space: String,
    externalId: String,
    createdTime: Option[Instant],
    lastUpdatedTime: Option[Instant],
    properties: Option[Map[String, Map[String, Map[String, InstancePropertyType]]]],
    startNode: Option[DirectRelationReference],
    endNode: Option[DirectRelationReference]
) extends InstanceDefinition {
  override val `type`: InstanceType = InstanceType.Edge
}

final case class InstancePropertyDefinition(
    identifier: String,
    nullable: Option[Boolean] = Some(true),
    autoIncrement: Option[Boolean] = Some(false),
    defaultValue: Option[PropertyDefaultValue],
    description: Option[String],
    name: Option[String],
    `type`: ContainerPropertyType
)

final case class InstanceRetrieveResponse(
    items: Seq[InstanceDefinition],
    typing: Option[Map[String, Map[String, Map[String, InstancePropertyDefinition]]]],
    nextCursor: Option[String]
)

final case class InstanceDeleteRequest(`type`: InstanceType, externalId: String, space: String)

final case class ViewPropertyReference(identifier: String, view: Option[ViewReference])

final case class PropertySortV3(
    property: ViewPropertyReference,
    direction: Option[SortDirection],
    nullsFirst: Option[Boolean]
)

final case class InstanceFilterRequest(
    sources: Seq[SourceReference],
    spaces: Option[Seq[String]],
    instanceType: Option[InstanceType],
    cursor: Option[String],
    limit: Option[Int],
    sort: Seq[PropertySortV3],
    filter: FilterDefinition
)
