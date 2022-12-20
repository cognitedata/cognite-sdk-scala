package com.cognite.sdk.scala.v1.fdm.instances

import cats.implicits.toTraverseOps
import com.cognite.sdk.scala.common._
import com.cognite.sdk.scala.v1.fdm.common.properties.{PrimitivePropType, PropertyType}
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
      properties: Option[Map[String, Map[String, Map[String, InstancePropertyValue]]]]
  ) extends InstanceDefinition {
    override val `type`: InstanceType = InstanceType.Node
  }

  final case class EdgeDefinition(
      relation: DirectRelationReference,
      space: String,
      externalId: String,
      createdTime: Option[Instant],
      lastUpdatedTime: Option[Instant],
      properties: Option[Map[String, Map[String, Map[String, InstancePropertyValue]]]],
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
  ): Decoder[Option[Map[String, Map[String, Map[String, InstancePropertyValue]]]]] = (c: HCursor) =>
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
                            Left[DecodingFailure, InstancePropertyValue](
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
              Map[String, Map[String, Map[String, InstancePropertyValue]]]
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
        .as[Option[Map[String, Map[String, Map[String, InstancePropertyValue]]]]](
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
        .as[Option[Map[String, Map[String, Map[String, InstancePropertyValue]]]]](
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

  private def toInstancePropertyType(
      instantPropTypeJson: Json,
      t: InstancePropertyDefinition
  ): Either[DecodingFailure, InstancePropertyValue] =
    t.`type` match {
      case PropertyType.DirectNodeRelationProperty(_) =>
        Decoder[Seq[String]]
          .decodeJson(instantPropTypeJson)
          .map(InstancePropertyValue.StringList.apply)
      case t if t.isList => toInstancePropertyTypeOfList(instantPropTypeJson, t)
      case t => toInstancePropertyTypeOfNonList(instantPropTypeJson, t)
    }

  private def toInstancePropertyTypeOfList(
      instantPropTypeJson: Json,
      t: PropertyType
  ): Either[DecodingFailure, InstancePropertyValue] =
    t match {
      case PropertyType.TextProperty(Some(true), _) =>
        Decoder[Seq[String]]
          .decodeJson(instantPropTypeJson)
          .map(InstancePropertyValue.StringList.apply)
      case PropertyType.PrimitiveProperty(PrimitivePropType.Boolean, Some(true)) =>
        Decoder[Seq[Boolean]]
          .decodeJson(instantPropTypeJson)
          .map(InstancePropertyValue.BooleanList.apply)
      case PropertyType.PrimitiveProperty(
            PrimitivePropType.Int32 | PrimitivePropType.Int64,
            Some(true)
          ) =>
        Decoder[Seq[Long]]
          .decodeJson(instantPropTypeJson)
          .map(InstancePropertyValue.IntegerList.apply)
      case PropertyType.PrimitiveProperty(
            PrimitivePropType.Float32 | PrimitivePropType.Float64,
            Some(true)
          ) =>
        Decoder[Seq[Double]]
          .decodeJson(instantPropTypeJson)
          .map(InstancePropertyValue.DoubleList.apply)
      case PropertyType.PrimitiveProperty(PrimitivePropType.Numeric, Some(true)) =>
        Decoder[Double]
          .decodeJson(instantPropTypeJson)
          .map(InstancePropertyValue.Double.apply)
      case PropertyType.PrimitiveProperty(PrimitivePropType.Date, Some(true)) =>
        Decoder[Seq[LocalDate]]
          .decodeJson(instantPropTypeJson)
          .map(InstancePropertyValue.DateList.apply)
      case PropertyType.PrimitiveProperty(PrimitivePropType.Timestamp, Some(true)) =>
        Decoder[Seq[ZonedDateTime]]
          .decodeJson(instantPropTypeJson)
          .map(InstancePropertyValue.TimestampList.apply)
      case PropertyType.PrimitiveProperty(PrimitivePropType.Json, Some(true)) =>
        Decoder[Seq[Json]]
          .decodeJson(instantPropTypeJson)
          .map(InstancePropertyValue.ObjectList.apply)
      case _ =>
        Left[DecodingFailure, InstancePropertyValue](
          DecodingFailure(
            s"Expected a list type, but found a non list type: ${instantPropTypeJson.noSpaces} as ${t.toString}",
            List.empty
          )
        )
    }

  private def toInstancePropertyTypeOfNonList(
      instantPropTypeJson: Json,
      t: PropertyType
  ): Either[DecodingFailure, InstancePropertyValue] =
    t match {
      case PropertyType.TextProperty(None | Some(false), _) =>
        Decoder[String]
          .decodeJson(instantPropTypeJson)
          .map(InstancePropertyValue.String.apply)
      case PropertyType.PrimitiveProperty(PrimitivePropType.Boolean, _) =>
        Decoder[Boolean]
          .decodeJson(instantPropTypeJson)
          .map(InstancePropertyValue.Boolean.apply)
      case PropertyType.PrimitiveProperty(
            PrimitivePropType.Int32 | PrimitivePropType.Int64,
            None | Some(false)
          ) =>
        Decoder[Long]
          .decodeJson(instantPropTypeJson)
          .map(InstancePropertyValue.Integer.apply)
      case PropertyType.PrimitiveProperty(
            PrimitivePropType.Float32 | PrimitivePropType.Float64,
            None | Some(false)
          ) =>
        Decoder[Double]
          .decodeJson(instantPropTypeJson)
          .map(InstancePropertyValue.Double.apply)
      case PropertyType.PrimitiveProperty(PrimitivePropType.Numeric, None | Some(false)) =>
        Decoder[Double]
          .decodeJson(instantPropTypeJson)
          .map(InstancePropertyValue.Double.apply)
      case PropertyType.PrimitiveProperty(PrimitivePropType.Date, None | Some(false)) =>
        Decoder[LocalDate]
          .decodeJson(instantPropTypeJson)
          .map(InstancePropertyValue.Date.apply)
      case PropertyType.PrimitiveProperty(PrimitivePropType.Timestamp, None | Some(false)) =>
        Decoder[ZonedDateTime]
          .decodeJson(instantPropTypeJson)
          .map(InstancePropertyValue.Timestamp.apply)
      case PropertyType.PrimitiveProperty(PrimitivePropType.Json, None | Some(false)) =>
        Decoder[Json]
          .decodeJson(instantPropTypeJson)
          .map(InstancePropertyValue.Object.apply)
      case _ =>
        Left[DecodingFailure, InstancePropertyValue](
          DecodingFailure(
            s"Expected a non list type, but found a list type: ${instantPropTypeJson.noSpaces} as ${t.toString}",
            List.empty
          )
        )
    }
}
