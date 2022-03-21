// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1

sealed trait PropertyType[T] {
  val getValue: T
}

sealed trait PropertyTypePrimitive[T] extends PropertyType[T]
final case class BooleanProperty(value: Boolean) extends PropertyTypePrimitive[Boolean] {
  override val getValue: Boolean = value
}
final case class Int32Property(value: Int) extends PropertyTypePrimitive[Int] {
  override val getValue: Int = value
}

final case class Int64Property(value: Long) extends PropertyTypePrimitive[Long] {
  override val getValue: Long = value
}

final case class Float32Property(value: Float) extends PropertyTypePrimitive[Float] {
  override val getValue: Float = value
}

final case class Float64Property(value: Double) extends PropertyTypePrimitive[Double] {
  override val getValue: Double = value
}
final case class StringProperty(value: String) extends PropertyTypePrimitive[String] {
  override val getValue: String = value
}

final case class ArrayProperty[+A <: PropertyTypePrimitive[A]](values: Vector[A])
    extends PropertyType[Vector[A]] {
  override val getValue: Vector[A] = values
}
