// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1.fdm.common.properties

import cats.implicits._
import com.cognite.sdk.scala.v1.fdm.containers.ContainerReference
import io.circe._
import io.circe.generic.semiauto._
import io.circe.syntax.EncoderOps

sealed trait PropertyType {
  def list: Option[Boolean]
  def isList: Boolean = list.getOrElse(false)
}

object PropertyType {

  final case class TextProperty(
      list: Option[Boolean] = Some(false),
      collation: Option[String] = Some("ucs_basic")
  ) extends PropertyType {
    val `type`: String = TextProperty.Type
  }
  object TextProperty {
    val Type = "text"
  }

  final case class PrimitiveProperty(`type`: PrimitivePropType, list: Option[Boolean] = Some(false))
      extends PropertyType

  final case class DirectNodeRelationProperty(container: Option[ContainerReference])
      extends PropertyType {
    val `type`: String = DirectNodeRelationProperty.Type

    override def list: Option[Boolean] = None
  }

  object DirectNodeRelationProperty {
    val Type = "direct"
  }

  import com.cognite.sdk.scala.v1.fdm.containers.ContainerReference._

  implicit val propertyTypeTextEncoder: Encoder[TextProperty] =
    Encoder.forProduct3("list", "collation", "type")((t: TextProperty) =>
      (t.list, t.collation, t.`type`)
    )

  implicit val primitivePropertyEncoder: Encoder[PrimitiveProperty] =
    deriveEncoder[PrimitiveProperty]

  implicit val directNodeRelationPropertyEncoder: Encoder[DirectNodeRelationProperty] =
    Encoder.forProduct2("container", "type")((d: DirectNodeRelationProperty) =>
      (d.container, d.`type`)
    )

  implicit val containerPropertyTypeEncoder: Encoder[PropertyType] = Encoder.instance {
    case t: TextProperty => t.asJson
    case p: PrimitiveProperty => p.asJson
    case d: DirectNodeRelationProperty => d.asJson
  }

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
          for {
            list <- c.downField("list").as[Option[Boolean]]
          } yield PrimitiveProperty(ppt, list)
      }

      val textPropertyOrDirectNodeRelationProperty = c.downField("type").as[String] match {
        case Left(err) => Left[DecodingFailure, PropertyType](err)
        case Right(typeVal) if typeVal === TextProperty.Type =>
          for {
            list <- c.downField("list").as[Option[Boolean]]
            collation <- c.downField("collation").as[Option[String]]
          } yield TextProperty(list, collation)
        case Right(typeVal) if typeVal === DirectNodeRelationProperty.Type =>
          for {
            containerRef <- c.downField("container").as[Option[ContainerReference]]
          } yield DirectNodeRelationProperty(containerRef)
        case Right(typeVal) =>
          Left[DecodingFailure, PropertyType](
            DecodingFailure(s"Unknown container property type: '$typeVal'", c.history)
          )
      }

      Seq(primitiveProperty, textPropertyOrDirectNodeRelationProperty).find(_.isRight) match {
        case Some(value) => value
        case None => Left(DecodingFailure(s"Unknown Property Type :${c.value.noSpaces}", c.history))
      }
    }
}