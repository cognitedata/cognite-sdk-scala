// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1.fdm.instances

import com.cognite.sdk.scala.v1.fdm.common.filters.FilterDefinition
import com.cognite.sdk.scala.v1.fdm.common.sources.SourceReference
import com.cognite.sdk.scala.v1.fdm.views.ViewReference

@deprecated
final case class EdgeOrNodeData(
    source: SourceReference,
    properties: Option[Map[String, Option[InstancePropertyValue]]]
)

@deprecated
final case class InstanceCreate(
    items: Seq[NodeOrEdgeCreate],
    autoCreateStartNodes: Option[Boolean] = Some(false),
    autoCreateEndNodes: Option[Boolean] = Some(false),
    skipOnVersionConflict: Option[Boolean] = Some(false),
    replace: Option[Boolean] = Some(false)
)

@deprecated
final case class InstanceRetrieve(
    instanceType: InstanceType,
    externalId: String,
    space: String
)

final case class InstanceSource(source: SourceReference)

@deprecated
final case class InstanceRetrieveRequest(
    items: Seq[InstanceRetrieve],
    includeTyping: Boolean,
    sources: Option[Seq[InstanceSource]]
)

@deprecated
final case class ViewPropertyReference(identifier: String, view: Option[ViewReference])

@deprecated
final case class PropertySortV3(
    property: ViewPropertyReference,
    direction: Option[SortDirection],
    nullsFirst: Option[Boolean]
)

@deprecated
final case class InstanceFilterRequest(
    sources: Option[Seq[InstanceSource]] = None,
    instanceType: Option[InstanceType] = None,
    cursor: Option[String] = None,
    limit: Option[Int] = None,
    sort: Option[Seq[PropertySortV3]] = None,
    filter: Option[FilterDefinition] = None,
    includeTyping: Option[Boolean]
)
