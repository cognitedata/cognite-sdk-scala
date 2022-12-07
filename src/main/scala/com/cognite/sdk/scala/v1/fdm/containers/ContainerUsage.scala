// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1.fdm.containers

import io.circe.{Decoder, Encoder}

import java.util.Locale

sealed abstract class ContainerUsage extends Product with Serializable

object ContainerUsage {

  case object Node extends ContainerUsage

  case object Edge extends ContainerUsage

  case object All extends ContainerUsage

  implicit val containerUsageEncoder: Encoder[ContainerUsage] =
    Encoder.instance[ContainerUsage](p =>
      io.circe.Json.fromString(p.productPrefix.toLowerCase(Locale.US))
    )

  implicit val containerUsageDecoder: Decoder[ContainerUsage] =
    Decoder[String].emap {
      case "node" => Right(Node)
      case "edge" => Right(Edge)
      case "all" => Right(All)
      case other => Left(s"Invalid Container Usage: $other")
    }
}
