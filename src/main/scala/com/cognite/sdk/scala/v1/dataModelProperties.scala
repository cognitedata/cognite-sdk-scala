// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1

import java.time.{LocalDate, ZonedDateTime}

sealed trait PropertyType

sealed trait PropertyTypePrimitive extends PropertyType
final case class BooleanProperty(value: Boolean) extends PropertyTypePrimitive
final case class Int32Property(value: Int) extends PropertyTypePrimitive
final case class Int64Property(value: Long) extends PropertyTypePrimitive
final case class Float32Property(value: Float) extends PropertyTypePrimitive
final case class Float64Property(value: Double) extends PropertyTypePrimitive
final case class StringProperty(value: String) extends PropertyTypePrimitive

final case class ArrayProperty[+A <: PropertyTypePrimitive](values: Vector[A]) extends PropertyType

final case class TimeStampProperty(value: ZonedDateTime) extends PropertyType
final case class DateProperty(value: LocalDate) extends PropertyType

// These types below are treated as string for now
final case class DirectRelationProperty(value: String) extends PropertyType
final case class GeometryProperty(value: String) extends PropertyType
final case class GeographyProperty(value: String) extends PropertyType

object PropertyName {
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
