// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1

import java.time.{LocalDate, ZonedDateTime}
// scalastyle:off number.of.types

sealed abstract class DataModelProperty[TV](val value: TV)

sealed abstract class PropertyType[TV] {
  sealed case class Property(override val value: TV) extends DataModelProperty(value)

  @SuppressWarnings(Array("org.wartremover.warts.PlatformDefault"))
  def code: String =
    toString.replaceAll("(.)([A-Z])", "$1_$2").toLowerCase
}

sealed abstract class PrimitivePropertyType[TV] extends PropertyType[TV]

sealed abstract class ArrayPropertyType[TV, TP <: PrimitivePropertyType[TV]](private val t: TP)
    extends PropertyType[Seq[TV]] {
  override def code: String =
    t.code + "[]"
}

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
  case object DirectRelation extends PropertyType[String]

  object Array {
    val values: Seq[PropertyType[_]] = Seq[PropertyType[_]](
      Boolean,
      Int,
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
    case object Int32 extends ArrayPropertyType[scala.Int, PropertyType.Int.type](PropertyType.Int)
    case object Int64
      extends ArrayPropertyType[scala.Long, PropertyType.Bigint.type](PropertyType.Bigint)
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
