package com.cognite.sdk.scala.v1.fdm.instances

import com.cognite.sdk.scala.common._
import com.cognite.sdk.scala.v1.resources.fdm.instances.Instances.{
  directRelationReferenceDecoder,
  directRelationReferenceEncoder
}
import io.circe.generic.semiauto.deriveDecoder
import io.circe.syntax.EncoderOps
import io.circe.{Decoder, DecodingFailure, Encoder, HCursor}
import java.time.Instant

sealed trait InstanceDefinition {
  val `type`: InstanceType
}

object InstanceDefinition {
  final case class NodeDefinition(
      space: String,
      externalId: String,
      createdTime: Option[Instant],
      lastUpdatedTime: Option[Instant],
      properties: Map[String, Map[String, Map[String, InstancePropertyType]]]
  ) extends InstanceDefinition {
    override val `type`: InstanceType = InstanceType.Node
  }

  final case class EdgeDefinition(
      relation: DirectRelationReference,
      space: String,
      externalId: String,
      createdTime: Option[Instant],
      lastUpdatedTime: Option[Instant],
      properties: Option[Map[String, Map[String, Map[String, InstancePropertyType]]]],
      startNode: Option[DirectRelationReference],
      endNode: Option[DirectRelationReference]
  ) extends InstanceDefinition {
    override val `type`: InstanceType = InstanceType.Edge
  }

  implicit val nodeDefinitionEncoder: Encoder[NodeDefinition] = Encoder.forProduct6(
    "type",
    "space",
    "externalId",
    "createdTime",
    "lastUpdatedTime",
    "properties"
  )((e: NodeDefinition) =>
    (e.`type`, e.space, e.externalId, e.createdTime, e.lastUpdatedTime, e.properties)
  )

  implicit val edgeDefinitionEncoder: Encoder[EdgeDefinition] = Encoder.forProduct7(
    "type",
    "relation",
    "space",
    "externalId",
    "createdTime",
    "lastUpdatedTime",
    "properties"
  )((e: EdgeDefinition) =>
    (e.`type`, e.relation, e.space, e.externalId, e.createdTime, e.lastUpdatedTime, e.properties)
  )

  implicit val instanceDefinitionEncoder: Encoder[InstanceDefinition] =
    Encoder.instance[InstanceDefinition] {
      case e: NodeDefinition => e.asJson
      case e: EdgeDefinition => e.asJson
    }

  implicit val nodeDefinitionDecoder: Decoder[NodeDefinition] = deriveDecoder

  implicit val edgeDefinitionDecoder: Decoder[EdgeDefinition] = deriveDecoder

  implicit val instanceDefinitionDecoder: Decoder[InstanceDefinition] = (c: HCursor) =>
    c.downField("type").as[InstanceType] match {
      case Left(err) => Left[DecodingFailure, InstanceDefinition](err)
      case Right(InstanceType.Node) => nodeDefinitionDecoder.apply(c)
      case Right(InstanceType.Edge) => edgeDefinitionDecoder.apply(c)
    }
}
