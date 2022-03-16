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
