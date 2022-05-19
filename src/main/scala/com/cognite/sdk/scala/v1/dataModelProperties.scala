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

object PropertyType {
  val Boolean = "boolean"
  val Int32 = "int32"
  val Int64 = "int64"
  val Int = "int"
  val Bigint = "bigint"
  val Float32 = "float32"
  val Float64 = "float64"
  val Numeric = "numeric"
  val Text = "text"
  val Timestamp = "timestamp"
  val Date = "date"

  val ArrayText = "text[]"
  val ArrayBoolean = "boolean[]"
  val ArrayInt32 = "int32[]"
  val ArrayInt64 = "int64[]"
  val ArrayInt = "int[]"
  val ArrayBigint = "bigint[]"
  val ArrayFloat32 = "float32[]"
  val ArrayFloat64 = "float64[]"
  val ArrayNumeric = "numeric[]"

  // These types below are treated as string for now
  val DirectRelation = "direct_relation"
  val Geometry = "geometry"
  val Geography = "geography"
}
