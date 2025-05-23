package com.cognite.sdk.scala.v1.fdm.instances

import io.circe.syntax.EncoderOps
import io.circe.{Decoder, DecodingFailure, Encoder, HCursor}
import com.cognite.sdk.scala.common._
import io.circe.generic.semiauto.deriveDecoder

import java.time.Instant

sealed abstract class SlimNodeOrEdge extends Product with Serializable {
  val space: String
  val externalId: String
  val instanceType: InstanceType
  val createdTime: Option[Instant]
  val lastUpdatedTime: Option[Instant]
  val wasModified: Boolean
}

object SlimNodeOrEdge {
  final case class SlimNodeDefinition(
      space: String,
      externalId: String,
      createdTime: Option[Instant],
      lastUpdatedTime: Option[Instant],
      wasModified: Boolean
  ) extends SlimNodeOrEdge {
    override val instanceType: InstanceType = InstanceType.Node
  }

  final case class SlimEdgeDefinition(
      space: String,
      externalId: String,
      createdTime: Option[Instant],
      lastUpdatedTime: Option[Instant],
      wasModified: Boolean
  ) extends SlimNodeOrEdge {
    override val instanceType: InstanceType = InstanceType.Edge
  }

  implicit val slimNodeDefinitionEncoder: Encoder[SlimNodeDefinition] =
    Encoder.forProduct6(
      "instanceType",
      "space",
      "externalId",
      "createdTime",
      "lastUpdatedTime",
      "wasModified"
    )((e: SlimNodeDefinition) =>
      (e.instanceType, e.space, e.externalId, e.createdTime, e.lastUpdatedTime, e.wasModified)
    )

  implicit val slimEdgeDefinitionEncoder: Encoder[SlimEdgeDefinition] =
    Encoder.forProduct6(
      "instanceType",
      "space",
      "externalId",
      "createdTime",
      "lastUpdatedTime",
      "wasModified"
    )((e: SlimEdgeDefinition) =>
      (e.instanceType, e.space, e.externalId, e.createdTime, e.lastUpdatedTime, e.wasModified)
    )

  implicit val slimNodeOrEdgeEncoder: Encoder[SlimNodeOrEdge] = Encoder.instance {
    case e: SlimNodeDefinition => e.asJson
    case e: SlimEdgeDefinition => e.asJson
  }

  implicit val slimNodeDefinitionDecoder: Decoder[SlimNodeDefinition] = deriveDecoder

  implicit val slimEdgeDefinitionDecoder: Decoder[SlimEdgeDefinition] = deriveDecoder

  implicit val slimNodeOrEdgeDecoder: Decoder[SlimNodeOrEdge] = (c: HCursor) =>
    c.downField("instanceType").as[InstanceType] match {
      case Left(err) => Left[DecodingFailure, SlimNodeOrEdge](err)
      case Right(InstanceType.Node) =>
        Decoder[SlimNodeDefinition].apply(c)
      case Right(InstanceType.Edge) =>
        Decoder[SlimEdgeDefinition].apply(c)
    }
}
