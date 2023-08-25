package com.cognite.sdk.scala.v1.fdm.instances

import io.circe.generic.semiauto.deriveDecoder
import io.circe.syntax.EncoderOps
import io.circe.{Decoder, DecodingFailure, Encoder, HCursor}

@deprecated("message", since = "0")
sealed trait InstanceDeletionRequest

@deprecated("message", since = "0")
object InstanceDeletionRequest {
  final case class NodeDeletionRequest(space: String, externalId: String)
      extends InstanceDeletionRequest {
    val instanceType: InstanceType = InstanceType.Node
  }

  final case class EdgeDeletionRequest(space: String, externalId: String)
      extends InstanceDeletionRequest {
    val instanceType: InstanceType = InstanceType.Edge
  }

  implicit val nodeDeletionRequestEncoder: Encoder[NodeDeletionRequest] =
    Encoder.forProduct3("instanceType", "space", "externalId")((e: NodeDeletionRequest) =>
      (e.instanceType, e.space, e.externalId)
    )

  implicit val edgeDeletionRequestEncoder: Encoder[EdgeDeletionRequest] =
    Encoder.forProduct3("instanceType", "space", "externalId")((e: EdgeDeletionRequest) =>
      (e.instanceType, e.space, e.externalId)
    )

  implicit val instanceDeletionRequestEncoder: Encoder[InstanceDeletionRequest] = Encoder.instance {
    case e: NodeDeletionRequest => e.asJson
    case e: EdgeDeletionRequest => e.asJson
  }

  implicit val nodeDeletionRequestDecoder: Decoder[NodeDeletionRequest] = deriveDecoder

  implicit val edgeDeletionRequestDecoder: Decoder[EdgeDeletionRequest] = deriveDecoder

  implicit val instanceDeletionRequestDecoder: Decoder[InstanceDeletionRequest] = (c: HCursor) =>
    c.downField("instanceType").as[InstanceType] match {
      case Left(err) => Left[DecodingFailure, InstanceDeletionRequest](err)
      case Right(InstanceType.Node) => Decoder[NodeDeletionRequest].apply(c)
      case Right(InstanceType.Edge) => Decoder[EdgeDeletionRequest].apply(c)
    }
}
