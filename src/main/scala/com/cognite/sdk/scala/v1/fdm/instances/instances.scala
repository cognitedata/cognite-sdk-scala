// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1.fdm.instances

import com.cognite.sdk.scala.v1.fdm.common.filters.FilterDefinition
import com.cognite.sdk.scala.v1.fdm.common.sources.SourceReference
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
    autoCreateStartNodes: Option[Boolean] = Some(false),
    autoCreateEndNodes: Option[Boolean] = Some(false),
    replace: Option[Boolean] = Some(false)
)

final case class InstanceRetrieve(
    instanceType: InstanceType,
    externalId: String,
    space: String,
    sources: Option[Seq[InstanceSource]]
)

final case class InstanceSource(source: SourceReference)

final case class InstanceRetrieveRequest(items: Seq[InstanceRetrieve], includeTyping: Boolean)

final case class ViewPropertyReference(identifier: String, view: Option[ViewReference])

final case class PropertySortV3(
    property: ViewPropertyReference,
    direction: Option[SortDirection],
    nullsFirst: Option[Boolean]
)

final case class InstanceFilterRequest(
    sources: Option[Seq[InstanceSource]] = None,
    instanceType: Option[InstanceType] = None,
    cursor: Option[String] = None,
    limit: Option[Int] = None,
    sort: Option[Seq[PropertySortV3]] = None,
    filter: Option[FilterDefinition] = None,
    includeTyping: Option[Boolean]
)
