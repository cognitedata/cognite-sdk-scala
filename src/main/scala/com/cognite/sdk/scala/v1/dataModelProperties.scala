// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1

import java.time.{LocalDate, ZonedDateTime}

sealed trait DataModelProperty

sealed trait DataModelPropertyPrimitive extends DataModelProperty
final case class BooleanProperty(value: Boolean) extends DataModelPropertyPrimitive
final case class Int32Property(value: Int) extends DataModelPropertyPrimitive
final case class Int64Property(value: Long) extends DataModelPropertyPrimitive
final case class Float32Property(value: Float) extends DataModelPropertyPrimitive
final case class Float64Property(value: Double) extends DataModelPropertyPrimitive
final case class StringProperty(value: String) extends DataModelPropertyPrimitive

final case class ArrayProperty[+A <: DataModelPropertyPrimitive](values: Vector[A]) extends DataModelProperty

final case class TimeStampProperty(value: ZonedDateTime) extends DataModelProperty
final case class DateProperty(value: LocalDate) extends DataModelProperty

// These types below are treated as string for now
final case class DirectRelationProperty(value: String) extends DataModelProperty
final case class GeometryProperty(value: String) extends DataModelProperty
final case class GeographyProperty(value: String) extends DataModelProperty

sealed abstract class PropertyType {

  @SuppressWarnings(Array("org.wartremover.warts.PlatformDefault"))
  def code: String =
    toString.replaceAll("(.)([A-Z])", "$1_$2").toLowerCase
}

object PropertyType {
  val values: Seq[PropertyType] = Seq[PropertyType](
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
    DirectRelation,
    Geometry,
    Geography
  ) ++
    Array.values

  private val _valuesMap: Map[String, PropertyType] =
    values.map(t => t.code -> t).toMap

  def fromCode(code: String): Option[PropertyType] =
    _valuesMap.get(code)

  case object Boolean extends PropertyType
  case object Int extends PropertyType
  case object Bigint extends PropertyType
  case object Float32 extends PropertyType
  case object Float64 extends PropertyType
  case object Numeric extends PropertyType
  case object Text extends PropertyType
  case object Json extends PropertyType
  case object Timestamp extends PropertyType
  case object Date extends PropertyType

  // These types below are treated as string for now
  case object DirectRelation extends PropertyType
  case object Geometry extends PropertyType
  case object Geography extends PropertyType

  sealed abstract class Array(val `type`: PropertyType) extends PropertyType {
    override def code: String =
      `type`.code + "[]"
  }

  object Array {
    val values: Seq[PropertyType] = Seq[PropertyType](
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

    case object Boolean extends Array(PropertyType.Boolean)
    case object Int extends Array(PropertyType.Int)
    case object Bigint extends Array(PropertyType.Bigint)
    case object Float32 extends Array(PropertyType.Float32)
    case object Float64 extends Array(PropertyType.Float64)
    case object Numeric extends Array(PropertyType.Numeric)
    case object Text extends Array(PropertyType.Text)
    case object Json extends Array(PropertyType.Json)
    case object Timestamp extends Array(PropertyType.Timestamp)
    case object Date extends Array(PropertyType.Date)

    case object Geometry extends Array(PropertyType.Geometry)
    case object Geography extends Array(PropertyType.Geography)
  }
}
