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

object PropertyType extends Enumeration {
  val Boolean = Value("boolean")
  val Int = Value("int")
  val Bigint = Value("bigint")
  val Float32 = Value("float32")
  val Float64 = Value("float64")
  val Numeric = Value("numeric")
  val Text = Value("text")
  val Json = Value("json")
  val Timestamp = Value("timestamp")
  val Date = Value("date")

  // These types below are treated as string for now
  val DirectRelation = Value("direct_relation")
  val Geometry = Value("geometry")
  val Geography = Value("geography")

  Array

  object Array {
    val Boolean = Value("boolean[]")
    val Int = Value("int[]")
    val Bigint = Value("bigint[]")
    val Float32 = Value("float32[]")
    val Float64 = Value("float64[]")
    val Numeric = Value("numeric[]")
    val Text = Value("text[]")
    val Json = Value("json[]")
    val Timestamp = Value("timestamp[]")
    val Date = Value("date[]")

    val Geometry = Value("geometry[]")
    val Geography = Value("geography[]")
  }

}
