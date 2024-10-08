// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1.fdm.common.properties

import cats.implicits._
import com.cognite.sdk.scala.v1.fdm.containers.ContainerReference
import com.cognite.sdk.scala.v1.fdm.views.ViewReference
import io.circe._
import io.circe.generic.semiauto._
import io.circe.syntax.EncoderOps

sealed trait ListablePropertyType extends PropertyType {
  def list: Option[Boolean]
  def isList: Boolean = list.getOrElse(false)
}

sealed trait PropertyType

object PropertyType {

  final case class TextProperty(
      list: Option[Boolean] = Some(false),
      collation: Option[String] = Some("ucs_basic")
  ) extends ListablePropertyType {
    val `type`: String = TextProperty.Type
  }

  object TextProperty {
    val Type = "text"
  }

  final case class PrimitiveProperty(`type`: PrimitivePropType, list: Option[Boolean] = Some(false))
      extends ListablePropertyType

  final case class DirectNodeRelationProperty(
      container: Option[ContainerReference],
      source: Option[ViewReference],
      list: Option[Boolean] = Some(false)
  ) extends ListablePropertyType {
    val `type`: String = DirectNodeRelationProperty.Type
  }

  object DirectNodeRelationProperty {
    val Type = "direct"
  }

  final case class EnumProperty(
      values: Map[String, EnumValueMetadata],
      unknownValue: Option[String]
  ) extends PropertyType {
    val `type`: String = EnumProperty.Type
  }

  final case class EnumValueMetadata(
      name: Option[String],
      description: Option[String]
  )

  object EnumProperty {
    val Type = "enum"
  }

  sealed abstract class CDFExternalIdReference extends ListablePropertyType {
    val `type`: String
  }

  final case class TimeSeriesReference(list: Option[Boolean] = None)
      extends CDFExternalIdReference {
    override val `type`: String = TimeSeriesReference.Type
  }

  object TimeSeriesReference {
    val Type = "timeseries"
  }

  final case class FileReference(list: Option[Boolean] = None) extends CDFExternalIdReference {
    override val `type`: String = FileReference.Type
  }

  object FileReference {
    val Type = "file"
  }

  final case class SequenceReference(list: Option[Boolean] = None) extends CDFExternalIdReference {
    override val `type`: String = SequenceReference.Type
  }

  object SequenceReference {
    val Type = "sequence"
  }

  import com.cognite.sdk.scala.v1.fdm.containers.ContainerReference._

  implicit val propertyTypeTextEncoder: Encoder[TextProperty] =
    Encoder.forProduct3("list", "collation", "type")((t: TextProperty) =>
      (t.list, t.collation, t.`type`)
    )

  implicit val externalIdReferenceEncoder: Encoder[CDFExternalIdReference] =
    Encoder.forProduct2("type", "list")(t => (t.`type`, t.list))

  implicit val primitivePropertyEncoder: Encoder[PrimitiveProperty] =
    deriveEncoder[PrimitiveProperty]

  implicit val directNodeRelationPropertyEncoder: Encoder[DirectNodeRelationProperty] =
    Encoder.forProduct4("type", "container", "source", "list")((d: DirectNodeRelationProperty) =>
      (d.`type`, d.container, d.source, d.list)
    )

  implicit val enumValueMetadataEncoder: Encoder[EnumValueMetadata] =
    Encoder.forProduct2("name", "description")((e: EnumValueMetadata) => (e.name, e.description))

  implicit val enumPropertyEncoder: Encoder[EnumProperty] =
    Encoder.forProduct3("type", "unknownValue", "values")((e: EnumProperty) =>
      (e.`type`, e.unknownValue, e.values)
    )

  implicit val containerPropertyTypeEncoder: Encoder[PropertyType] = Encoder.instance {
    case t: TextProperty => t.asJson
    case p: PrimitiveProperty => p.asJson
    case d: DirectNodeRelationProperty => d.asJson
    case ts: CDFExternalIdReference => ts.asJson
    case e: EnumProperty => e.asJson
  }

  implicit val enumPropertyDecoder: Decoder[EnumProperty] =
    deriveDecoder[EnumProperty]

  implicit val enumValueMetadataDecoder: Decoder[EnumValueMetadata] =
    deriveDecoder[EnumValueMetadata]

  implicit val primitivePropertyDecoder: Decoder[PrimitiveProperty] =
    deriveDecoder[PrimitiveProperty]

  implicit val textPropertyDecoder: Decoder[TextProperty] =
    deriveDecoder[TextProperty]

  implicit val directNodeRelationPropertyDecoder: Decoder[DirectNodeRelationProperty] =
    deriveDecoder[DirectNodeRelationProperty]

  implicit val containerPropertyTypeDecoder: Decoder[PropertyType] =
    Decoder.instance { (c: HCursor) =>
      val primitiveProperty = c.downField("type").as[PrimitivePropType] match {
        case Left(err) => Left[DecodingFailure, PropertyType](err)
        case Right(ppt: PrimitivePropType) =>
          c.downField("list").as[Option[Boolean]].map(PrimitiveProperty(ppt, _))
      }

      val nonPrimitiveProperty =
        c.downField("type").as[String] match {
          case Left(err) => Left[DecodingFailure, PropertyType](err)
          case Right(typeVal) if typeVal === EnumProperty.Type =>
            for {
              values <- c.downField("values").as[Map[String, EnumValueMetadata]]
              unknownValue <- c.downField("unknownValue").as[Option[String]]
            } yield EnumProperty(values, unknownValue)
          case Right(typeVal) if typeVal === TextProperty.Type =>
            for {
              list <- c.downField("list").as[Option[Boolean]]
              collation <- c.downField("collation").as[Option[String]]
            } yield TextProperty(list, collation)
          case Right(typeVal) if typeVal === DirectNodeRelationProperty.Type =>
            for {
              containerRef <- c.downField("container").as[Option[ContainerReference]]
              source <- c.downField("source").as[Option[ViewReference]]
              list <- c.downField("list").as[Option[Boolean]]
            } yield DirectNodeRelationProperty(containerRef, source, list)
          case Right(typeVal) if typeVal === TimeSeriesReference.Type =>
            c.downField("list").as[Option[Boolean]].map(TimeSeriesReference(_))
          case Right(typeVal) if typeVal === FileReference.Type =>
            c.downField("list").as[Option[Boolean]].map(FileReference(_))
          case Right(typeVal) if typeVal === SequenceReference.Type =>
            c.downField("list").as[Option[Boolean]].map(SequenceReference(_))
          case Right(typeVal) =>
            Left[DecodingFailure, PropertyType](
              DecodingFailure(s"Unknown container property type: '$typeVal'", c.history)
            )
        }

      Seq(primitiveProperty, nonPrimitiveProperty).find(
        _.isRight
      ) match {
        case Some(value) => value
        case None => Left(DecodingFailure(s"Unknown Property Type :${c.value.noSpaces}", c.history))
      }
    }
}
