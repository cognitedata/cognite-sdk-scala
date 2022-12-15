// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1

import java.time.{LocalDate, ZonedDateTime}
import io.circe.{ACursor, Decoder, Encoder, Json}
import io.circe.syntax._

// scalastyle:off number.of.types

sealed abstract case class DataModelProperty[V](value: V)(implicit encoder: Encoder[V]) {
  import io.circe.parser._
  def encode: Json =
    value match {
      case rawStringJson: String =>
        parse(rawStringJson) match {
          case Left(_) => value.asJson
          case Right(json) => json
        }
      case _ => value.asJson
    }
}

sealed abstract class PropertyType[V](implicit decoder: Decoder[V], encoder: Encoder[V])
    extends Serializable {
  sealed class Property(override val value: V) extends DataModelProperty[V](value)
  object Property {
    def apply(v: V): Property = new Property(v)
    def unapply(p: Property): Option[V] = Some(p.value)
  }

  @SuppressWarnings(Array("org.wartremover.warts.PlatformDefault"))
  def code: String =
    toString.replaceAll("(.)([A-Z])", "$1_$2").toLowerCase

  def decodeProperty(c: ACursor): Decoder.Result[Property] =
    c.as[V].map(Property(_))
}

sealed abstract class PrimitivePropertyType[V](implicit decoder: Decoder[V], encoder: Encoder[V])
    extends PropertyType[V]

sealed abstract class ArrayPropertyType[V, P <: PrimitivePropertyType[V]](private val t: P)(
    implicit decoder: Decoder[Seq[V]],
    encoder: Encoder[Seq[V]]
) extends PropertyType[Seq[V]] {
  override def code: String =
    t.code + "[]"
}

// There are a lot of property types, but it can't be helped.
// scalastyle:off number.of.types
object PropertyType {

  val values: Seq[PropertyType[_]] = Seq[PropertyType[_]](
    Boolean,
    Int,
    Int32,
    Int64,
    Bigint,
    Float32,
    Float64,
    Numeric,
    Text,
    Json,
    Timestamp,
    Date,
    Geometry,
    Geography,
    DirectRelation
  ) ++
    Array.values

  private val valuesMap: Map[String, PropertyType[_]] =
    values.map(t => t.code -> t).toMap

  def fromCode(code: String): Option[PropertyType[_]] =
    valuesMap.get(code)

  case object Boolean extends PrimitivePropertyType[scala.Boolean]
  case object Int extends PrimitivePropertyType[scala.Int]
  case object Int32 extends PrimitivePropertyType[scala.Int]
  case object Int64 extends PrimitivePropertyType[scala.Long]
  case object Bigint extends PrimitivePropertyType[scala.Long]
  case object Float32 extends PrimitivePropertyType[scala.Float]
  case object Float64 extends PrimitivePropertyType[scala.Double]
  case object Numeric extends PrimitivePropertyType[scala.BigDecimal]
  case object Text extends PrimitivePropertyType[String]
  case object Json extends PrimitivePropertyType[String]
  case object Timestamp extends PrimitivePropertyType[ZonedDateTime]
  case object Date extends PrimitivePropertyType[LocalDate]
  case object Geometry extends PrimitivePropertyType[String]
  case object Geography extends PrimitivePropertyType[String]
  case object DirectRelation extends PrimitivePropertyType[Seq[String]]

  object Array {
    val values: Seq[PropertyType[_]] = Seq[PropertyType[_]](
      Boolean,
      Int,
      Int32,
      Int64,
      Bigint,
      Float32,
      Float64,
      Numeric,
      Text,
      Json,
      Timestamp,
      Date,
      Geometry,
      Geography
    )
    case object Boolean
        extends ArrayPropertyType[scala.Boolean, PropertyType.Boolean.type](PropertyType.Boolean)
    case object Int extends ArrayPropertyType[scala.Int, PropertyType.Int.type](PropertyType.Int)
    case object Int32
        extends ArrayPropertyType[scala.Int, PropertyType.Int32.type](PropertyType.Int32)
    case object Int64
        extends ArrayPropertyType[scala.Long, PropertyType.Int64.type](PropertyType.Int64)
    case object Bigint
        extends ArrayPropertyType[scala.Long, PropertyType.Bigint.type](PropertyType.Bigint)
    case object Float32
        extends ArrayPropertyType[scala.Float, PropertyType.Float32.type](PropertyType.Float32)
    case object Float64
        extends ArrayPropertyType[scala.Double, PropertyType.Float64.type](PropertyType.Float64)
    case object Numeric
        extends ArrayPropertyType[scala.BigDecimal, PropertyType.Numeric.type](
          PropertyType.Numeric
        )
    case object Text extends ArrayPropertyType[String, PropertyType.Text.type](PropertyType.Text)
    case object Json extends ArrayPropertyType[String, PropertyType.Json.type](PropertyType.Json)
    case object Timestamp
        extends ArrayPropertyType[ZonedDateTime, PropertyType.Timestamp.type](
          PropertyType.Timestamp
        )
    case object Date extends ArrayPropertyType[LocalDate, PropertyType.Date.type](PropertyType.Date)
    case object Geometry
        extends ArrayPropertyType[String, PropertyType.Geometry.type](PropertyType.Geometry)
    case object Geography
        extends ArrayPropertyType[String, PropertyType.Geography.type](PropertyType.Geography)
  }
}
// scalastyle:on number.of.types
