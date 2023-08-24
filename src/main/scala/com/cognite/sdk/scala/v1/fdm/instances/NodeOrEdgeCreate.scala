// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1.fdm.instances

import com.cognite.sdk.scala.v1.fdm.common.DirectRelationReference
import com.cognite.sdk.scala.v1.fdm.instances.InstanceType._
import io.circe.generic.semiauto.deriveDecoder
import io.circe.syntax.EncoderOps
import io.circe.{Decoder, DecodingFailure, Encoder, HCursor}

@deprecated
sealed abstract class NodeOrEdgeCreate extends Product with Serializable {
  val space: String
  val externalId: String
}

@deprecated
object NodeOrEdgeCreate {

  final case class NodeWrite(
      space: String,
      externalId: String,
      sources: Option[Seq[EdgeOrNodeData]]
  ) extends NodeOrEdgeCreate {
    val instanceType: InstanceType = Node
  }

  final case class EdgeWrite(
      // This is to represent the node that is behind every edge. The value of `type`.externalId can be anything.
      // This is exposed to discourage unnecessary edge creation.
      // Therefore we have to exposing it to the end user, although it can be auto populated.
      // More info: https://cognitedata.slack.com/archives/C031G8Y19HP/p1670333909605369
      `type`: DirectRelationReference,
      space: String,
      externalId: String,
      startNode: DirectRelationReference,
      endNode: DirectRelationReference,
      sources: Option[Seq[EdgeOrNodeData]]
  ) extends NodeOrEdgeCreate {
    val instanceType: InstanceType = Edge
  }

  import com.cognite.sdk.scala.v1.resources.fdm.instances.Instances._

  implicit val nodeWriteEncoder: Encoder[NodeWrite] =
    Encoder.forProduct4("instanceType", "space", "externalId", "sources")((e: NodeWrite) =>
      (e.instanceType, e.space, e.externalId, e.sources)
    )

  implicit val edgeWriteEncoder: Encoder[EdgeWrite] =
    Encoder.forProduct7(
      "instanceType",
      "type",
      "space",
      "externalId",
      "startNode",
      "endNode",
      "sources"
    )((e: EdgeWrite) =>
      (e.instanceType, e.`type`, e.space, e.externalId, e.startNode, e.endNode, e.sources)
    )

  implicit val instanceTypeWriteItemEncoder: Encoder[NodeOrEdgeCreate] =
    Encoder.instance[NodeOrEdgeCreate] {
      case e: NodeWrite => e.asJson
      case e: EdgeWrite => e.asJson
    }

  implicit val nodeWriteDecoder: Decoder[NodeWrite] = deriveDecoder

  implicit val edgeWriteDecoder: Decoder[EdgeWrite] = deriveDecoder

  implicit val instanceTypeWriteItemDecoder: Decoder[NodeOrEdgeCreate] = (c: HCursor) =>
    c.downField("instanceType").as[InstanceType] match {
      case Left(err) => Left[DecodingFailure, NodeOrEdgeCreate](err)
      case Right(InstanceType.Node) => nodeWriteDecoder.apply(c)
      case Right(InstanceType.Edge) => edgeWriteDecoder.apply(c)
    }

}
