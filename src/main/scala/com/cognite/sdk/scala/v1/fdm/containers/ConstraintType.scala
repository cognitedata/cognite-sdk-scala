// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1.fdm.containers

import io.circe._

import java.util.Locale

@deprecated
sealed abstract class ConstraintType extends Product with Serializable

@deprecated
object ConstraintType {
  case object Unique extends ConstraintType

  case object Required extends ConstraintType

  implicit val constraintTypeDecoder: Decoder[ConstraintType] = Decoder[String].emap {
    case "unique" => Right(Unique)
    case "required" => Right(Required)
    case other => Left(s"Invalid Constraint Type: $other")
  }

  implicit val constraintTypeEncoder: Encoder[ConstraintType] =
    Encoder.instance[ConstraintType](p =>
      io.circe.Json.fromString(p.productPrefix.toLowerCase(Locale.US))
    )
}
