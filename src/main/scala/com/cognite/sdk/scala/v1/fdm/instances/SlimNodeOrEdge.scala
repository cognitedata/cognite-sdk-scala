package com.cognite.sdk.scala.v1.fdm.instances

import io.circe.syntax.EncoderOps
import io.circe.{Decoder, DecodingFailure, Encoder, HCursor}
import com.cognite.sdk.scala.common._
import io.circe.generic.semiauto.deriveDecoder

import java.time.Instant

sealed abstract class SlimNodeOrEdge extends Product with Serializable {
  val `type`: InstanceType
}

object SlimNodeOrEdge {
  final case class SlimNodeDefinition(
      space: String,
      externalId: String,
      createdTime: Option[Instant],
      lastUpdatedTime: Option[Instant]
  ) extends SlimNodeOrEdge {
    override val `type`: InstanceType = InstanceType.Node
  }

  final case class SlimEdgeDefinition(
      space: String,
      externalId: String,
      createdTime: Option[Instant],
      lastUpdatedTime: Option[Instant]
  ) extends SlimNodeOrEdge {
    override val `type`: InstanceType = InstanceType.Edge
  }

  implicit val slimNodeDefinitionEncoder: Encoder[SlimNodeDefinition] =
    Encoder.forProduct5("type", "space", "externalId", "createdTime", "lastUpdatedTime")(
      (e: SlimNodeDefinition) => (e.`type`, e.space, e.externalId, e.createdTime, e.lastUpdatedTime)
    )

  implicit val slimEdgeDefinitionEncoder: Encoder[SlimEdgeDefinition] =
    Encoder.forProduct5("type", "space", "externalId", "createdTime", "lastUpdatedTime")(
      (e: SlimEdgeDefinition) => (e.`type`, e.space, e.externalId, e.createdTime, e.lastUpdatedTime)
    )

  implicit val slimNodeOrEdgeEncoder: Encoder[SlimNodeOrEdge] = Encoder.instance {
    case e: SlimNodeDefinition => e.asJson
    case e: SlimEdgeDefinition => e.asJson
  }

  implicit val slimNodeDefinitionDecoder: Decoder[SlimNodeDefinition] = deriveDecoder

  implicit val slimEdgeDefinitionDecoder: Decoder[SlimEdgeDefinition] = deriveDecoder

  implicit val slimNodeOrEdgeDecoder: Decoder[SlimNodeOrEdge] = (c: HCursor) =>
    c.downField("type").as[InstanceType] match {
      case Left(err) => Left[DecodingFailure, SlimNodeOrEdge](err)
      case Right(InstanceType.Node) =>
        Decoder[SlimNodeDefinition].apply(c)
      case Right(InstanceType.Edge) =>
        Decoder[SlimEdgeDefinition].apply(c)
      case Right(typeValue) =>
        Left[DecodingFailure, SlimNodeOrEdge](
          DecodingFailure(s"Unknown Instant Type: ${typeValue.toString}", c.history)
        )
    }
}
