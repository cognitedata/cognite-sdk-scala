package com.cognite.sdk.scala.v1.fdm.instances

import cats.implicits.toTraverseOps
import com.cognite.sdk.scala.common._
import com.cognite.sdk.scala.v1.fdm.containers.{ContainerPropertyType, PrimitivePropType}
import com.cognite.sdk.scala.v1.resources.fdm.instances.Instances.{
  directRelationReferenceDecoder,
  directRelationReferenceEncoder
}
import io.circe._
import io.circe.generic.semiauto.deriveDecoder
import io.circe.syntax.EncoderOps

import java.time.{Instant, LocalDate, ZonedDateTime}

sealed trait InstanceDefinition {
  val `type`: InstanceType
}

object InstanceDefinition {
  final case class NodeDefinition(
      space: String,
      externalId: String,
      createdTime: Option[Instant],
      lastUpdatedTime: Option[Instant],
      properties: Option[Map[String, Map[String, Map[String, InstancePropertyType]]]]
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

  private val nodeDefinitionDecoder: Decoder[NodeDefinition] = deriveDecoder

  private val edgeDefinitionDecoder: Decoder[EdgeDefinition] = deriveDecoder

  def instancePropertyDefinitionBasedInstanceDefinitionDecoder(
      propertyDefinitionsMap: Option[
        Map[String, Map[String, Map[String, InstancePropertyDefinition]]]
      ]
  ): Decoder[InstanceDefinition] = (c: HCursor) =>
    propertyDefinitionsMap match {
      case Some(propDefMap) =>
        c.downField("type").as[InstanceType] match {
          case Left(err) => Left[DecodingFailure, InstanceDefinition](err)
          case Right(InstanceType.Node) =>
            instancePropertyDefinitionBasedNodeDefinitionDecoder(propDefMap).apply(c)
          case Right(InstanceType.Edge) =>
            instancePropertyDefinitionBasedEdgeDefinitionDecoder(propDefMap).apply(c)
        }
      case None =>
        c.downField("type").as[InstanceType] match {
          case Left(err) => Left[DecodingFailure, InstanceDefinition](err)
          case Right(InstanceType.Node) => nodeDefinitionDecoder.apply(c)
          case Right(InstanceType.Edge) => edgeDefinitionDecoder.apply(c)
        }
    }

  private def instancePropertyDefinitionBasedInstancePropertyTypeDecoder(
      types: Map[String, Map[String, Map[String, InstancePropertyDefinition]]]
  ): Decoder[Option[Map[String, Map[String, Map[String, InstancePropertyType]]]]] = (c: HCursor) =>
    {
      val result = c.value
        .as[Option[Map[String, Map[String, Map[String, Json]]]]]
        .flatMap {
          case Some(propertiesAsJson) =>
            val propertiesTyped = propertiesAsJson.toList
              .traverse { case (spaceName, spacePropsMap) =>
                val spacePropsMapTyped = spacePropsMap.toList
                  .traverse { case (viewOrContainerId, viewOrContainerPropsMap) =>
                    val viewOrContainerPropsMapTyped = viewOrContainerPropsMap.toList
                      .traverse { case (propId, instantPropTypeJson) =>
                        val typeDef = types
                          .get(spaceName)
                          .flatMap(_.get(viewOrContainerId).flatMap(_.get(propId)))
                        val typedInstancePropType = typeDef match {
                          case Some(t) => toInstancePropertyType(instantPropTypeJson, t)
                          case None =>
                            Left[DecodingFailure, InstancePropertyType](
                              DecodingFailure(
                                s"Couldn't find InstancePropertyDefinition for property $spaceName.$viewOrContainerId.$propId",
                                c.history
                              )
                            )
                        }
                        typedInstancePropType.map(ipt => propId -> ipt)
                      }
                      .map(m => viewOrContainerId -> m.toMap)
                    viewOrContainerPropsMapTyped
                  }
                  .map(m => spaceName -> m.toMap)
                spacePropsMapTyped
              }
              .map(m => Some(m.toMap))

            propertiesTyped

          case None =>
            Right[DecodingFailure, Option[
              Map[String, Map[String, Map[String, InstancePropertyType]]]
            ]](None)
        }

      result
    }

  private def instancePropertyDefinitionBasedNodeDefinitionDecoder(
      instPropDefMap: Map[String, Map[String, Map[String, InstancePropertyDefinition]]]
  ): Decoder[NodeDefinition] = (c: HCursor) =>
    for {
      space <- c.downField("space").as[String]
      externalId <- c.downField("externalId").as[String]
      createdTime <- c.downField("createdTime").as[Option[Instant]]
      lastUpdatedTime <- c.downField("lastUpdatedTime").as[Option[Instant]]
      properties <- c
        .downField("properties")
        .as[Option[Map[String, Map[String, Map[String, InstancePropertyType]]]]](
          instancePropertyDefinitionBasedInstancePropertyTypeDecoder(instPropDefMap)
        )
    } yield NodeDefinition(
      space = space,
      externalId = externalId,
      createdTime = createdTime,
      lastUpdatedTime = lastUpdatedTime,
      properties = properties
    )

  private def instancePropertyDefinitionBasedEdgeDefinitionDecoder(
      instPropDefMap: Map[String, Map[String, Map[String, InstancePropertyDefinition]]]
  ): Decoder[EdgeDefinition] = (c: HCursor) =>
    for {
      relation <- c.downField("relation").as[DirectRelationReference]
      space <- c.downField("space").as[String]
      externalId <- c.downField("externalId").as[String]
      createdTime <- c.downField("createdTime").as[Option[Instant]]
      lastUpdatedTime <- c.downField("lastUpdatedTime").as[Option[Instant]]
      properties <- c
        .downField("properties")
        .as[Option[Map[String, Map[String, Map[String, InstancePropertyType]]]]](
          instancePropertyDefinitionBasedInstancePropertyTypeDecoder(instPropDefMap)
        )
      startNode <- c.downField("startNode").as[Option[DirectRelationReference]]
      endNode <- c.downField("endNode").as[Option[DirectRelationReference]]
    } yield EdgeDefinition(
      relation = relation,
      space = space,
      externalId = externalId,
      createdTime = createdTime,
      lastUpdatedTime = lastUpdatedTime,
      properties = properties,
      startNode = startNode,
      endNode = endNode
    )

  // scalastyle:off
  private def toInstancePropertyType(
      instantPropTypeJson: Json,
      t: InstancePropertyDefinition
  ): Either[DecodingFailure, InstancePropertyType] =
    t.`type` match {
      // List types
      case ContainerPropertyType.TextProperty(Some(true), _) =>
        Decoder[Seq[String]]
          .decodeJson(instantPropTypeJson)
          .map(InstancePropertyType.StringList.apply)
      case ContainerPropertyType.PrimitiveProperty(PrimitivePropType.Boolean, Some(true)) =>
        Decoder[Seq[Boolean]]
          .decodeJson(instantPropTypeJson)
          .map(InstancePropertyType.BooleanList.apply)
      case ContainerPropertyType.PrimitiveProperty(
            PrimitivePropType.Int32 | PrimitivePropType.Int64,
            Some(true)
          ) =>
        Decoder[Seq[Long]]
          .decodeJson(instantPropTypeJson)
          .map(InstancePropertyType.IntegerList.apply)
      case ContainerPropertyType.PrimitiveProperty(
            PrimitivePropType.Float32 | PrimitivePropType.Float64,
            Some(true)
          ) =>
        Decoder[Seq[Double]]
          .decodeJson(instantPropTypeJson)
          .map(InstancePropertyType.DoubleList.apply)
      case ContainerPropertyType.PrimitiveProperty(PrimitivePropType.Numeric, Some(true)) =>
        Decoder[Double]
          .decodeJson(instantPropTypeJson)
          .map(InstancePropertyType.Double.apply)
      case ContainerPropertyType.PrimitiveProperty(PrimitivePropType.Date, Some(true)) =>
        Decoder[Seq[LocalDate]]
          .decodeJson(instantPropTypeJson)
          .map(InstancePropertyType.DateList.apply)
      case ContainerPropertyType.PrimitiveProperty(PrimitivePropType.Timestamp, Some(true)) =>
        Decoder[Seq[ZonedDateTime]]
          .decodeJson(instantPropTypeJson)
          .map(InstancePropertyType.TimestampList.apply)
      case ContainerPropertyType.PrimitiveProperty(PrimitivePropType.Json, Some(true)) =>
        Decoder[Seq[Json]]
          .decodeJson(instantPropTypeJson)
          .map(InstancePropertyType.ObjectsList.apply)

      // non-list types
      case ContainerPropertyType.TextProperty(_, _) =>
        Decoder[String]
          .decodeJson(instantPropTypeJson)
          .map(InstancePropertyType.String.apply)
      case ContainerPropertyType.PrimitiveProperty(PrimitivePropType.Boolean, _) =>
        Decoder[Boolean]
          .decodeJson(instantPropTypeJson)
          .map(InstancePropertyType.Boolean.apply)
      case ContainerPropertyType.PrimitiveProperty(
            PrimitivePropType.Int32 | PrimitivePropType.Int64,
            _
          ) =>
        Decoder[Long]
          .decodeJson(instantPropTypeJson)
          .map(InstancePropertyType.Integer.apply)
      case ContainerPropertyType.PrimitiveProperty(
            PrimitivePropType.Float32 | PrimitivePropType.Float64,
            _
          ) =>
        Decoder[Double]
          .decodeJson(instantPropTypeJson)
          .map(InstancePropertyType.Double.apply)
      case ContainerPropertyType.PrimitiveProperty(PrimitivePropType.Numeric, _) =>
        Decoder[Double]
          .decodeJson(instantPropTypeJson)
          .map(InstancePropertyType.Double.apply)
      case ContainerPropertyType.PrimitiveProperty(PrimitivePropType.Date, _) =>
        Decoder[LocalDate]
          .decodeJson(instantPropTypeJson)
          .map(InstancePropertyType.Date.apply)
      case ContainerPropertyType.PrimitiveProperty(PrimitivePropType.Timestamp, _) =>
        Decoder[ZonedDateTime]
          .decodeJson(instantPropTypeJson)
          .map(InstancePropertyType.Timestamp.apply)
      case ContainerPropertyType.PrimitiveProperty(PrimitivePropType.Json, _) =>
        Decoder[Json]
          .decodeJson(instantPropTypeJson)
          .map(InstancePropertyType.Object.apply)
      case ContainerPropertyType.DirectNodeRelationProperty(_) =>
        Decoder[Seq[String]]
          .decodeJson(instantPropTypeJson)
          .map(InstancePropertyType.StringList.apply)
    }
  // scalastyle:on
}
