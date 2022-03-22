// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1

sealed trait PropertyType

sealed trait PropertyTypePrimitive extends PropertyType
final case class BooleanProperty(value: Boolean) extends PropertyTypePrimitive
final case class Int32Property(value: Int) extends PropertyTypePrimitive
final case class Int64Property(value: Long) extends PropertyTypePrimitive
final case class Float32Property(value: Float) extends PropertyTypePrimitive
final case class Float64Property(value: Double) extends PropertyTypePrimitive
final case class StringProperty(value: String) extends PropertyTypePrimitive

final case class ArrayProperty[+A <: PropertyTypePrimitive](values: Vector[A]) extends PropertyType

// These types below are treated as string for now
final case class DirectRelationProperty(value: String) extends PropertyType
final case class TimeStampProperty(value: String) extends PropertyType
final case class DateProperty(value: String) extends PropertyType
final case class GeometryProperty(value: String) extends PropertyType
final case class GeographyProperty(value: String) extends PropertyType

object PropertyName {
  val boolean = "boolean"
  val int32 = "int32"
  val int64 = "int64"
  val int = "int"
  val bigint = "bigint"
  val float32 = "float32"
  val float64 = "float64"
  val numeric = "numeric"
  val text = "text"

  val arrayText = "text[]"
  val arrayBoolean = "boolean[]"
  val arrayInt32 = "int32[]"
  val arrayInt64 = "int64[]"
  val arrayInt = "int[]"
  val arrayBigint = "bigint[]"
  val arrayFloat32 = "float32[]"
  val arrayFloat64 = "float64[]"
  val arrayNumeric = "numeric[]"

  // These types below are treated as string for now
  val directRelation = "direct_relation"
  val timestamp = "timestamp"
  val date = "date"
  val geometry = "geometry"
  val geography = "geography"
}
