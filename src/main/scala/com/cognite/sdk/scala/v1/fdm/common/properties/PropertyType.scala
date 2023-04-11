// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1.fdm.common.properties

import cats.implicits._
import com.cognite.sdk.scala.v1.fdm.containers.ContainerReference
import com.cognite.sdk.scala.v1.fdm.views.ViewReference
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

  final case class DirectNodeRelationProperty(
      container: Option[ContainerReference],
      source: Option[ViewReference]
  ) extends PropertyType {
    val `type`: String = DirectNodeRelationProperty.Type

    override def list: Option[Boolean] = None
  }

  object DirectNodeRelationProperty {
    val Type = "direct"
  }

  final case class TimeSeriesProperty() extends PropertyType {
    val `type`: String = TimeSeriesProperty.Type

    override def list: Option[Boolean] = None
  }

  object TimeSeriesProperty {
    val Type = "timeseries"
  }

  import com.cognite.sdk.scala.v1.fdm.containers.ContainerReference._

  implicit val propertyTypeTextEncoder: Encoder[TextProperty] =
    Encoder.forProduct3("list", "collation", "type")((t: TextProperty) =>
      (t.list, t.collation, t.`type`)
    )

  implicit val propertyTimeSeriesEncoder: Encoder[TimeSeriesProperty] =
    Encoder.forProduct1("type")((t: TimeSeriesProperty) => t.`type`)

  implicit val primitivePropertyEncoder: Encoder[PrimitiveProperty] =
    deriveEncoder[PrimitiveProperty]

  implicit val directNodeRelationPropertyEncoder: Encoder[DirectNodeRelationProperty] =
    Encoder.forProduct3("type", "container", "source")((d: DirectNodeRelationProperty) =>
      (d.`type`, d.container, d.source)
    )

  implicit val containerPropertyTypeEncoder: Encoder[PropertyType] = Encoder.instance {
    case t: TextProperty => t.asJson
    case p: PrimitiveProperty => p.asJson
    case d: DirectNodeRelationProperty => d.asJson
    case ts: TimeSeriesProperty => ts.asJson
  }

  implicit val primitivePropertyDecoder: Decoder[PrimitiveProperty] =
    deriveDecoder[PrimitiveProperty]

  implicit val textPropertyDecoder: Decoder[TextProperty] =
    deriveDecoder[TextProperty]

  implicit val timeSeriesPropertyDecoder: Decoder[TimeSeriesProperty] =
    deriveDecoder[TimeSeriesProperty]

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

      val textPropertyOrDirectNodeRelationPropertyOrTimeSeriesProperty =
        c.downField("type").as[String] match {
          case Left(err) => Left[DecodingFailure, PropertyType](err)
          case Right(typeVal) if typeVal === TextProperty.Type =>
            for {
              list <- c.downField("list").as[Option[Boolean]]
              collation <- c.downField("collation").as[Option[String]]
            } yield TextProperty(list, collation)
          case Right(typeVal) if typeVal === DirectNodeRelationProperty.Type =>
            for {
              containerRef <- c.downField("container").as[Option[ContainerReference]]
              source <- c.downField("source").as[Option[ViewReference]]
            } yield DirectNodeRelationProperty(containerRef, source)
          case Right(typeVal) if typeVal === TimeSeriesProperty.Type =>
            for { _ <- c.downField("type").as[Option[String]] } yield TimeSeriesProperty()
          case Right(typeVal) =>
            Left[DecodingFailure, PropertyType](
              DecodingFailure(s"Unknown container property type: '$typeVal'", c.history)
            )
        }

      Seq(primitiveProperty, textPropertyOrDirectNodeRelationPropertyOrTimeSeriesProperty).find(
        _.isRight
      ) match {
        case Some(value) => value
        case None => Left(DecodingFailure(s"Unknown Property Type :${c.value.noSpaces}", c.history))
      }
    }
}
