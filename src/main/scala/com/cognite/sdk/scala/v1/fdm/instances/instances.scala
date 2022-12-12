// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1.fdm.instances

import com.cognite.sdk.scala.v1.fdm.common.properties.{PropertyDefaultValue, PropertyType}
import com.cognite.sdk.scala.v1.fdm.common.filters.FilterDefinition
import com.cognite.sdk.scala.v1.fdm.common.refs.SourceReference
import com.cognite.sdk.scala.v1.fdm.containers.ContainerReference
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

final case class EdgeOrNodeData(
    source: SourceReference,
    properties: Option[Map[String, InstancePropertyValue]]
)

final case class InstanceCreate(
    items: Seq[NodeOrEdgeCreate],
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
    `type`: PropertyType
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
