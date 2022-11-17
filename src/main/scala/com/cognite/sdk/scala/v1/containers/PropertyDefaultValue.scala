// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1.containers

import io.circe._

import java.util.Locale

sealed abstract class PropertyDefaultValue extends Product with Serializable

object PropertyDefaultValue {
  case object String extends PropertyDefaultValue

  case object Number extends PropertyDefaultValue

  case object Boolean extends PropertyDefaultValue

  case object Object extends PropertyDefaultValue

  implicit val propertyDefaultValueEncoder: Encoder[PropertyDefaultValue] =
    Encoder.instance[PropertyDefaultValue](p =>
      io.circe.Json.fromString(p.productPrefix.toLowerCase(Locale.US))
    )

  implicit val propertyDefaultValueDecoder: Decoder[PropertyDefaultValue] =
    Decoder[String].emap {
      case "string" => Right(String)
      case "number" => Right(Number)
      case "boolean" => Right(Boolean)
      case "object" => Right(Object)
      case other => Left(s"Invalid Property Default Value: $other")
    }
}
