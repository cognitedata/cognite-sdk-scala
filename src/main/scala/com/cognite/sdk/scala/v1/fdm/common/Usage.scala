// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1.fdm.common

import io.circe.{Decoder, Encoder}

import java.util.Locale

sealed abstract class Usage extends Product with Serializable

object Usage {

  case object Node extends Usage

  case object Edge extends Usage

  case object All extends Usage

  implicit val containerUsageEncoder: Encoder[Usage] =
    Encoder.instance[Usage](p => io.circe.Json.fromString(p.productPrefix.toLowerCase(Locale.US)))

  implicit val containerUsageDecoder: Decoder[Usage] =
    Decoder[String].emap {
      case "node" => Right(Node)
      case "edge" => Right(Edge)
      case "all" => Right(All)
      case other => Left(s"Invalid Container Usage: $other")
    }
}
