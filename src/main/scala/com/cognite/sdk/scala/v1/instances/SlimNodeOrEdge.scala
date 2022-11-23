package com.cognite.sdk.scala.v1.instances

import io.circe.syntax.EncoderOps
import io.circe.{Decoder, DecodingFailure, Encoder, HCursor}

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

  implicit val slimNodeDefinitionDecoder: Decoder[SlimNodeOrEdge] = (c: HCursor) =>
    c.downField("type").as[InstanceType] match {
      case Left(err) => Left[DecodingFailure, SlimNodeOrEdge](err)
      case Right(typeValue) if typeValue == InstanceType.Node =>
        for {
          space <- c.downField("space").as[String]
          externalId <- c.downField("externalId").as[String]
          createdTime <- c.downField("createdTime").as[Option[Instant]]
          lastUpdatedTime <- c.downField("lastUpdatedTime").as[Option[Instant]]
        } yield SlimNodeDefinition(space, externalId, createdTime, lastUpdatedTime)
      case Right(typeValue) if typeValue == InstanceType.Edge =>
        for {
          space <- c.downField("space").as[String]
          externalId <- c.downField("externalId").as[String]
          createdTime <- c.downField("createdTime").as[Option[Instant]]
          lastUpdatedTime <- c.downField("lastUpdatedTime").as[Option[Instant]]
        } yield SlimEdgeDefinition(space, externalId, createdTime, lastUpdatedTime)
      case Right(typeValue) =>
        Left[DecodingFailure, SlimNodeOrEdge](
          DecodingFailure(s"Unknown Instant Type: ${typeValue}", c.history)
        )
    }
}
