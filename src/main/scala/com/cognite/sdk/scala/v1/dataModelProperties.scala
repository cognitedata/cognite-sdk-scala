// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1

sealed trait PropertyType[T] {
  val value: T
}

sealed trait PropertyTypePrimitive[T] extends PropertyType[T]
final case class BooleanProperty(override val value: Boolean) extends PropertyTypePrimitive[Boolean]
final case class Int32Property(override val value: Int) extends PropertyTypePrimitive[Int]
final case class Int64Property(override val value: Long) extends PropertyTypePrimitive[Long]
final case class Float32Property(override val value: Float) extends PropertyTypePrimitive[Float]
final case class Float64Property(override val value: Double) extends PropertyTypePrimitive[Double]
final case class StringProperty(override val value: String) extends PropertyTypePrimitive[String]
final case class ArrayProperty[+A <: PropertyTypePrimitive[T], T](values: Vector[A])
    extends PropertyType[Vector[T]] {
  override val value: Vector[T] = values.map(_.value)
}
