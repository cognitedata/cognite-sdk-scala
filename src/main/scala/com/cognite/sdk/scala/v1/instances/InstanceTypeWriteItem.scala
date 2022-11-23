// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1.instances

import com.cognite.sdk.scala.v1.instances.InstanceType.{Edge, Node}
import io.circe.Encoder
import io.circe.syntax.EncoderOps

sealed abstract class InstanceTypeWriteItem extends Product with Serializable

object InstanceTypeWriteItem {
  final case class NodeViewWriteItem(
      space: String,
      externalId: String,
      views: Seq[InstanceViewData]
  ) extends InstanceTypeWriteItem {
    val instanceType: InstanceType = Node
  }

  final case class NodeContainerWriteItem(
      space: String,
      externalId: String,
      containers: Seq[InstanceContainerData]
  ) extends InstanceTypeWriteItem {
    val instanceType: InstanceType = Node
  }

  final case class EdgeViewWriteItem(
      `type`: DirectRelationReference,
      space: String,
      externalId: String,
      startNode: DirectRelationReference,
      endNode: DirectRelationReference,
      views: Seq[InstanceViewData]
  ) extends InstanceTypeWriteItem {
    val instanceType: InstanceType = Edge
  }

  final case class EdgeContainerWriteItem(
      `type`: DirectRelationReference,
      space: String,
      externalId: String,
      startNode: DirectRelationReference,
      endNode: DirectRelationReference,
      containers: Seq[InstanceContainerData]
  ) extends InstanceTypeWriteItem {
    val instanceType: InstanceType = Edge
  }

  import com.cognite.sdk.scala.v1.resources.Instances._

  implicit val nodeViewWriteItemEncoder: Encoder[NodeViewWriteItem] =
    Encoder.forProduct4("instanceType", "space", "externalId", "views")((e: NodeViewWriteItem) =>
      (e.instanceType, e.space, e.externalId, e.views)
    )

  implicit val nodeContainerWriteItemEncoder: Encoder[NodeContainerWriteItem] =
    Encoder.forProduct4("instanceType", "space", "externalId", "containers")(
      (e: NodeContainerWriteItem) => (e.instanceType, e.space, e.externalId, e.containers)
    )

  implicit val edgeViewWriteItemEncoder: Encoder[EdgeViewWriteItem] =
    Encoder.forProduct7(
      "instanceType",
      "type",
      "space",
      "externalId",
      "startNode",
      "endNode",
      "views"
    )((e: EdgeViewWriteItem) =>
      (e.instanceType, e.`type`, e.space, e.externalId, e.startNode, e.endNode, e.views)
    )

  implicit val edgeContainerWriteItemEncoder: Encoder[EdgeContainerWriteItem] =
    Encoder.forProduct7(
      "instanceType",
      "type",
      "space",
      "externalId",
      "startNode",
      "endNode",
      "containers"
    )((e: EdgeContainerWriteItem) =>
      (e.instanceType, e.`type`, e.space, e.externalId, e.startNode, e.endNode, e.containers)
    )

  implicit val instanceTypeWriteItemEncoder: Encoder[InstanceTypeWriteItem] =
    Encoder.instance[InstanceTypeWriteItem] {
      case e: NodeViewWriteItem => e.asJson
      case e: NodeContainerWriteItem => e.asJson
      case e: EdgeViewWriteItem => e.asJson
      case e: EdgeContainerWriteItem => e.asJson
    }

}
