// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1.fdm.instances

import com.cognite.sdk.scala.v1.fdm.common.filters.FilterDefinition
import com.cognite.sdk.scala.v1.fdm.common.sources.SourceReference
import com.cognite.sdk.scala.v1.fdm.views.ViewReference

final case class EdgeOrNodeData(
    source: SourceReference,
    properties: Option[Map[String, Option[InstancePropertyValue]]]
)

final case class InstanceCreate(
    items: Seq[NodeOrEdgeCreate],
    autoCreateStartNodes: Option[Boolean] = Some(false),
    autoCreateEndNodes: Option[Boolean] = Some(false),
    skipOnVersionConflict: Option[Boolean] = Some(false),
    replace: Option[Boolean] = Some(false)
)

final case class InstanceRetrieve(
    instanceType: InstanceType,
    externalId: String,
    space: String
)

final case class InstanceSource(source: SourceReference)

final case class InstanceRetrieveRequest(
    items: Seq[InstanceRetrieve],
    includeTyping: Boolean,
    sources: Option[Seq[InstanceSource]]
)

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

final case class InstanceSyncRequest(
    `with`: Map[String, TableExpression] = Map.empty,
    cursors: Option[Map[String, String]] = None,
    select: Map[String, SelectExpression] = Map.empty
)

final case class TableExpression(
    limit: Option[Int] = Option(1000),
    nodes: Option[NodesTableExpression] = None,
    edges: Option[EdgeTableExpression] = None
)

final case class NodesTableExpression(
    from: Option[String] = None,
    chainTo: Option[String] = Option("destination"),
    direction: Option[String] = Option("inwards"),
    filter: Option[FilterDefinition] = None,
    through: Option[ViewPropertyReference] = None
)

final case class EdgeTableExpression(
     from: Option[String] = None,
     chainTo: Option[String] = Option("destination"),
     maxDistance: Option[Int] = None,
     direction: Option[String] = Option("inwards"),
     filter: Option[FilterDefinition] = None,
     nodeFilter: Option[FilterDefinition] = None,
     terminationFilter: Option[FilterDefinition] = None
)

final case class SelectExpression(
    sources: Seq[SourceSelector] = Seq.empty
)

final case class SourceSelector(
    source: SourceReference,
    properties: Seq[String] = Seq.empty
)
