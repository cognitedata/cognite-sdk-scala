// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1.containers

import cats.implicits._
import io.circe._
import io.circe.generic.semiauto._

sealed trait ContainerPropertyType

object ContainerPropertyType {

  final case class TextProperty(
      list: Option[Boolean] = Some(false),
      collation: Option[String] = Some("ucs_basic")
  ) extends ContainerPropertyType {
    val `type`: String = TextProperty.Type
  }
  object TextProperty {
    val Type = "text"
  }

  final case class PrimitiveProperty(`type`: PrimitivePropType, list: Option[Boolean] = Some(false))
      extends ContainerPropertyType

  final case class DirectNodeRelationProperty(container: Option[ContainerReference])
      extends ContainerPropertyType {
    val `type`: String = DirectNodeRelationProperty.Type
  }
  object DirectNodeRelationProperty {
    val Type = "direct"
  }

  import com.cognite.sdk.scala.v1.containers.ContainerReference._

  implicit val propertyTypeTextEncoder: Encoder[TextProperty] =
    Encoder.forProduct3("list", "collation", "type")((t: TextProperty) =>
      (t.list, t.collation, t.`type`)
    )

  implicit val propertyTypeTextDecoder: Decoder[TextProperty] =
    deriveDecoder[TextProperty]

  implicit val primitivePropertyEncoder: Encoder[PrimitiveProperty] =
    deriveEncoder[PrimitiveProperty]

  implicit val primitivePropertyDecoder: Decoder[PrimitiveProperty] =
    deriveDecoder[PrimitiveProperty]

  implicit val directNodeRelationPropertyEncoder: Encoder[DirectNodeRelationProperty] =
    Encoder.forProduct2("container", "type")((d: DirectNodeRelationProperty) =>
      (d.container, d.`type`)
    )

  implicit val directNodeRelationPropertyDecoder: Decoder[DirectNodeRelationProperty] =
    deriveDecoder[DirectNodeRelationProperty]

  implicit val containerPropertyTypeEncoder: Encoder[ContainerPropertyType] = Encoder.instance {
    case t: TextProperty => Encoder[TextProperty].apply(t)
    case p: PrimitiveProperty => Encoder[PrimitiveProperty].apply(p)
    case d: DirectNodeRelationProperty => Encoder[DirectNodeRelationProperty].apply(d)
  }

  implicit val containerPropertyTypeDecoder: Decoder[ContainerPropertyType] =
    Decoder.instance { (c: HCursor) =>
      val primitiveProperty = c.downField("type").as[PrimitivePropType] match {
        case Left(err) => Left[DecodingFailure, ContainerPropertyType](err)
        case Right(ppt: PrimitivePropType) =>
          for {
            list <- c.downField("list").as[Option[Boolean]]
          } yield PrimitiveProperty(ppt, list)
      }

      primitiveProperty.handleErrorWith { (_: DecodingFailure) =>
        c.downField("type").as[String] match {
          case Left(err) => Left[DecodingFailure, ContainerPropertyType](err)
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
            Left[DecodingFailure, ContainerPropertyType](
              DecodingFailure(s"Unknown container property type: '$typeVal'", c.history)
            )
        }
      }
    }
}
