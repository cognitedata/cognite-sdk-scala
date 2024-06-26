package com.cognite.sdk.scala.v1.fdm.instances

import cats.implicits._
import com.cognite.sdk.scala.v1.fdm.common.DirectRelationReference
import com.cognite.sdk.scala.v1.fdm.common.properties.{PrimitivePropType, PropertyType}
import io.circe._
import io.circe.generic.semiauto.deriveDecoder
import io.circe.syntax.EncoderOps

import java.time.{LocalDate, ZonedDateTime}

sealed trait InstanceDefinition {
  def space: String
  def externalId: String
  def createdTime: Long
  def lastUpdatedTime: Long
  def deletedTime: Option[Long]
  def version: Option[Long]
  def properties: Option[Map[String, Map[String, Map[String, InstancePropertyValue]]]]

  val instanceType: InstanceType
}

object InstanceDefinition {
  final case class NodeDefinition(
      space: String,
      externalId: String,
      createdTime: Long,
      lastUpdatedTime: Long,
      deletedTime: Option[Long],
      version: Option[Long],
      properties: Option[Map[String, Map[String, Map[String, InstancePropertyValue]]]],
      `type`: Option[DirectRelationReference]
  ) extends InstanceDefinition {
    override val instanceType: InstanceType = InstanceType.Node
  }

  final case class EdgeDefinition(
      `type`: DirectRelationReference,
      space: String,
      externalId: String,
      createdTime: Long,
      lastUpdatedTime: Long,
      deletedTime: Option[Long],
      version: Option[Long],
      properties: Option[Map[String, Map[String, Map[String, InstancePropertyValue]]]],
      startNode: DirectRelationReference,
      endNode: DirectRelationReference
  ) extends InstanceDefinition {
    override val instanceType: InstanceType = InstanceType.Edge
  }

  implicit val nodeDefinitionEncoder: Encoder[NodeDefinition] = Encoder.forProduct8(
    "instanceType",
    "space",
    "externalId",
    "createdTime",
    "lastUpdatedTime",
    "deletedTime",
    "version",
    "properties"
  )((e: NodeDefinition) =>
    (
      e.instanceType,
      e.space,
      e.externalId,
      e.createdTime,
      e.lastUpdatedTime,
      e.deletedTime,
      e.version,
      e.properties
    )
  )

  implicit val edgeDefinitionEncoder: Encoder[EdgeDefinition] = Encoder.forProduct9(
    "instanceType",
    "type",
    "space",
    "externalId",
    "createdTime",
    "lastUpdatedTime",
    "deletedTime",
    "version",
    "properties"
  )((e: EdgeDefinition) =>
    (
      e.instanceType,
      e.`type`,
      e.space,
      e.externalId,
      e.createdTime,
      e.lastUpdatedTime,
      e.deletedTime,
      e.version,
      e.properties
    )
  )

  implicit val instanceDefinitionEncoder: Encoder[InstanceDefinition] =
    Encoder.instance[InstanceDefinition] {
      case e: NodeDefinition => e.asJson
      case e: EdgeDefinition => e.asJson
    }

  private val nodeDefinitionDecoder: Decoder[NodeDefinition] = deriveDecoder

  private val edgeDefinitionDecoder: Decoder[EdgeDefinition] = deriveDecoder

  def instancePropertyDefinitionBasedInstanceDecoder(
      propertyTypeDefinitionsMap: Option[
        Map[String, Map[String, Map[String, TypePropertyDefinition]]]
      ]
  ): Decoder[InstanceDefinition] = (c: HCursor) =>
    propertyTypeDefinitionsMap match {
      case Some(propDefMap) =>
        c.downField("instanceType").as[InstanceType] match {
          case Left(err) => Left[DecodingFailure, InstanceDefinition](err)
          case Right(InstanceType.Node) =>
            instancePropertyDefinitionBasedNodeDefinitionDecoder(propDefMap).apply(c)
          case Right(InstanceType.Edge) =>
            instancePropertyDefinitionBasedEdgeDefinitionDecoder(propDefMap).apply(c)
        }
      case None =>
        c.downField("instanceType").as[InstanceType] match {
          case Left(err) => Left[DecodingFailure, InstanceDefinition](err)
          case Right(InstanceType.Node) => nodeDefinitionDecoder.apply(c)
          case Right(InstanceType.Edge) => edgeDefinitionDecoder.apply(c)
        }
    }

  def instancePropertyDefinitionBasedInstancePropertyTypeDecoder(
      types: Map[String, Map[String, Map[String, TypePropertyDefinition]]]
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
      instPropDefMap: Map[String, Map[String, Map[String, TypePropertyDefinition]]]
  ): Decoder[NodeDefinition] = (c: HCursor) =>
    for {
      space <- c.downField("space").as[String]
      externalId <- c.downField("externalId").as[String]
      createdTime <- c.downField("createdTime").as[Long]
      lastUpdatedTime <- c.downField("lastUpdatedTime").as[Long]
      deletedTime <- c.downField("deletedTime").as[Option[Long]]
      version <- c.downField("version").as[Option[Long]]
      properties <- c
        .downField("properties")
        .as[Option[Map[String, Map[String, Map[String, InstancePropertyValue]]]]](
          instancePropertyDefinitionBasedInstancePropertyTypeDecoder(instPropDefMap)
        )
      relation <- c.downField("type").as[Option[DirectRelationReference]]
    } yield NodeDefinition(
      space = space,
      externalId = externalId,
      createdTime = createdTime,
      lastUpdatedTime = lastUpdatedTime,
      deletedTime = deletedTime,
      version = version,
      properties = properties,
      `type` = relation
    )

  private def instancePropertyDefinitionBasedEdgeDefinitionDecoder(
      instPropDefMap: Map[String, Map[String, Map[String, TypePropertyDefinition]]]
  ): Decoder[EdgeDefinition] = (c: HCursor) =>
    for {
      relation <- c.downField("type").as[DirectRelationReference]
      space <- c.downField("space").as[String]
      externalId <- c.downField("externalId").as[String]
      createdTime <- c.downField("createdTime").as[Long]
      lastUpdatedTime <- c.downField("lastUpdatedTime").as[Long]
      deletedTime <- c.downField("deletedTime").as[Option[Long]]
      version <- c.downField("version").as[Option[Long]]
      properties <- c
        .downField("properties")
        .as[Option[Map[String, Map[String, Map[String, InstancePropertyValue]]]]](
          instancePropertyDefinitionBasedInstancePropertyTypeDecoder(instPropDefMap)
        )
      startNode <- c.downField("startNode").as[DirectRelationReference]
      endNode <- c.downField("endNode").as[DirectRelationReference]
    } yield EdgeDefinition(
      `type` = relation,
      space = space,
      externalId = externalId,
      createdTime = createdTime,
      lastUpdatedTime = lastUpdatedTime,
      deletedTime = deletedTime,
      version = version,
      properties = properties,
      startNode = startNode,
      endNode = endNode
    )

  private def toInstancePropertyType(
      propValue: Json,
      t: TypePropertyDefinition
  ): Either[DecodingFailure, InstancePropertyValue] =
    t.`type` match {
      case t if t.isList => toInstancePropertyTypeOfList(propValue, t)
      case t => toInstancePropertyTypeOfNonList(propValue, t)
    }

  // scalastyle:off cyclomatic.complexity method.length
  private def toInstancePropertyTypeOfList(
      propValue: Json,
      t: PropertyType
  ): Either[DecodingFailure, InstancePropertyValue] =
    t match {
      case PropertyType.TextProperty(Some(true), _) =>
        Decoder[Seq[String]]
          .decodeJson(propValue)
          .map(InstancePropertyValue.StringList.apply)
      case PropertyType.PrimitiveProperty(PrimitivePropType.Boolean, Some(true)) =>
        Decoder[Seq[Boolean]]
          .decodeJson(propValue)
          .map(InstancePropertyValue.BooleanList.apply)
      case PropertyType.PrimitiveProperty(PrimitivePropType.Int32, Some(true)) =>
        Decoder[Seq[Int]]
          .decodeJson(propValue)
          .map(InstancePropertyValue.Int32List.apply)
      case PropertyType.PrimitiveProperty(PrimitivePropType.Int64, Some(true)) =>
        Decoder[Seq[Long]]
          .decodeJson(propValue)
          .map(InstancePropertyValue.Int64List.apply)
      case PropertyType.PrimitiveProperty(PrimitivePropType.Float32, Some(true)) =>
        Decoder[Seq[Float]]
          .decodeJson(propValue)
          .map(InstancePropertyValue.Float32List.apply)
      case PropertyType.PrimitiveProperty(PrimitivePropType.Float64, Some(true)) =>
        Decoder[Seq[Double]]
          .decodeJson(propValue)
          .map(InstancePropertyValue.Float64List.apply)
      case PropertyType.PrimitiveProperty(PrimitivePropType.Date, Some(true)) =>
        Decoder[Seq[LocalDate]]
          .decodeJson(propValue)
          .map(InstancePropertyValue.DateList.apply)
      case PropertyType.PrimitiveProperty(PrimitivePropType.Timestamp, Some(true)) =>
        Decoder[Seq[ZonedDateTime]]
          .decodeJson(propValue)
          .map(InstancePropertyValue.TimestampList.apply)
      case PropertyType.PrimitiveProperty(PrimitivePropType.Json, Some(true)) =>
        Decoder[Seq[Json]]
          .decodeJson(propValue)
          .map(InstancePropertyValue.ObjectList.apply)
      case PropertyType.TimeSeriesReference(Some(true)) =>
        Decoder[Seq[String]]
          .decodeJson(propValue)
          .map(InstancePropertyValue.TimeSeriesReferenceList.apply)
      case PropertyType.FileReference(Some(true)) =>
        Decoder[Seq[String]]
          .decodeJson(propValue)
          .map(InstancePropertyValue.FileReferenceList.apply)
      case PropertyType.SequenceReference(Some(true)) =>
        Decoder[Seq[String]]
          .decodeJson(propValue)
          .map(InstancePropertyValue.SequenceReferenceList.apply)
      case PropertyType.DirectNodeRelationProperty(_, _, Some(true)) =>
        Decoder[Seq[DirectRelationReference]]
          .decodeJson(propValue)
          .map(InstancePropertyValue.ViewDirectNodeRelationList.apply)
      case _ =>
        Left[DecodingFailure, InstancePropertyValue](
          DecodingFailure(
            s"Expected a list type, but found a non list type: ${propValue.noSpaces} as ${t.toString}",
            List.empty
          )
        )
    }
  // scalastyle:on cyclomatic.complexity method.length

  // scalastyle:off cyclomatic.complexity method.length
  private def toInstancePropertyTypeOfNonList(
      propValue: Json,
      t: PropertyType
  ): Either[DecodingFailure, InstancePropertyValue] =
    t match {
      case PropertyType.TextProperty(None | Some(false), _) =>
        Decoder[String]
          .decodeJson(propValue)
          .map(InstancePropertyValue.String.apply)
      case PropertyType.PrimitiveProperty(PrimitivePropType.Boolean, _) =>
        Decoder[Boolean]
          .decodeJson(propValue)
          .map(InstancePropertyValue.Boolean.apply)
      case PropertyType.PrimitiveProperty(PrimitivePropType.Int32, None | Some(false)) =>
        Decoder[Int]
          .decodeJson(propValue)
          .map(InstancePropertyValue.Int32.apply)
      case PropertyType.PrimitiveProperty(PrimitivePropType.Int64, None | Some(false)) =>
        Decoder[Long]
          .decodeJson(propValue)
          .map(InstancePropertyValue.Int64.apply)
      case PropertyType.PrimitiveProperty(PrimitivePropType.Float32, None | Some(false)) =>
        Decoder[Float]
          .decodeJson(propValue)
          .map(InstancePropertyValue.Float32.apply)
      case PropertyType.PrimitiveProperty(PrimitivePropType.Float64, None | Some(false)) =>
        Decoder[Double]
          .decodeJson(propValue)
          .map(InstancePropertyValue.Float64.apply)
      case PropertyType.PrimitiveProperty(PrimitivePropType.Date, None | Some(false)) =>
        Decoder[LocalDate]
          .decodeJson(propValue)
          .map(InstancePropertyValue.Date.apply)
      case PropertyType.PrimitiveProperty(PrimitivePropType.Timestamp, None | Some(false)) =>
        Decoder[ZonedDateTime]
          .decodeJson(propValue)
          .map(InstancePropertyValue.Timestamp.apply)
      case PropertyType.PrimitiveProperty(PrimitivePropType.Json, None | Some(false)) =>
        Decoder[Json]
          .decodeJson(propValue)
          .map(InstancePropertyValue.Object.apply)
      case PropertyType.TimeSeriesReference(None | Some(false)) =>
        Decoder[String]
          .decodeJson(propValue)
          .map(InstancePropertyValue.TimeSeriesReference.apply)
      case PropertyType.FileReference(None | Some(false)) =>
        Decoder[String]
          .decodeJson(propValue)
          .map(InstancePropertyValue.FileReference.apply)
      case PropertyType.SequenceReference(None | Some(false)) =>
        Decoder[String]
          .decodeJson(propValue)
          .map(InstancePropertyValue.SequenceReference.apply)
      case PropertyType.DirectNodeRelationProperty(_, _, Some(false)) |
          PropertyType.DirectNodeRelationProperty(_, _, None) =>
        propValue
          .as[Option[DirectRelationReference]]
          .map(InstancePropertyValue.ViewDirectNodeRelation.apply)
      case _ =>
        Left[DecodingFailure, InstancePropertyValue](
          DecodingFailure(
            s"Expected a non list type, but found a list type: ${propValue.noSpaces} as ${t.toString}",
            List.empty
          )
        )
    }
  // scalastyle:on cyclomatic.complexity method.length
}
