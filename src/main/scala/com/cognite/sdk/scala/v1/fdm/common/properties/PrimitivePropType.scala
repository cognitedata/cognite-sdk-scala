// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1.fdm.common.properties

import io.circe.{Decoder, Encoder}

import java.util.Locale

sealed abstract class PrimitivePropType extends Product with Serializable

object PrimitivePropType {

  case object Boolean extends PrimitivePropType

  case object Float32 extends PrimitivePropType

  case object Float64 extends PrimitivePropType

  case object Int32 extends PrimitivePropType

  case object Int64 extends PrimitivePropType

  case object Timestamp extends PrimitivePropType

  case object Date extends PrimitivePropType

  case object Json extends PrimitivePropType

  implicit val primitivePropTypeDecoder: Decoder[PrimitivePropType] = Decoder[String].emap {
    case "boolean" => Right(Boolean)
    case "float32" => Right(Float32)
    case "float64" => Right(Float64)
    case "int32" => Right(Int32)
    case "int64" => Right(Int64)
    case "timestamp" => Right(Timestamp)
    case "date" => Right(Date)
    case "json" => Right(Json)
    case other => Left(s"Invalid Primitive Property Type: $other")
  }

  implicit val primitivePropTypeEncoder: Encoder[PrimitivePropType] =
    Encoder.instance[PrimitivePropType](p =>
      io.circe.Json.fromString(p.productPrefix.toLowerCase(Locale.US))
    )
}
