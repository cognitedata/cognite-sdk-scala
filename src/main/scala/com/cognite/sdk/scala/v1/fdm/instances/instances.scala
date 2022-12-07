// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1.fdm.instances

import com.cognite.sdk.scala.v1.fdm.SourceReference
import com.cognite.sdk.scala.v1.fdm.containers.{
  ContainerPropertyType,
  ContainerReference,
  PropertyDefaultValue
}
import com.cognite.sdk.scala.v1.fdm.views.ViewReference

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

final case class InstanceRetrieve(
    sources: Seq[SourceReference],
    instanceType: InstanceType,
    externalId: String,
    space: String
)

final case class InstanceRetrieveRequest(items: Seq[InstanceRetrieve], includeTyping: Boolean)

final case class InstancePropertyDefinition(
    identifier: String,
    nullable: Option[Boolean] = Some(true),
    autoIncrement: Option[Boolean] = Some(false),
    defaultValue: Option[PropertyDefaultValue],
    description: Option[String],
    name: Option[String],
    `type`: ContainerPropertyType
)

final case class InstanceFilterResponse(
    items: Seq[InstanceDefinition],
    typing: Option[Map[String, Map[String, Map[String, InstancePropertyDefinition]]]],
    nextCursor: Option[String]
)

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
