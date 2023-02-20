package com.cognite.sdk.scala.v1.fdm.views

import io.circe.{Decoder, Encoder}

import java.util.Locale

sealed abstract class ConnectionDirection extends Product with Serializable

object ConnectionDirection {
  case object Outwards extends ConnectionDirection
  case object Inwards extends ConnectionDirection

  implicit val connectionDirectionDecoder: Decoder[ConnectionDirection] = Decoder[String].emap {
    case "outwards" => Right(Outwards)
    case "inwards" => Right(Inwards)
    case other => Left(s"Invalid Connection direction: $other")
  }

  implicit val connectionDirectionEncoder: Encoder[ConnectionDirection] =
    Encoder.instance[ConnectionDirection](p =>
      io.circe.Json.fromString(p.productPrefix.toLowerCase(Locale.US))
    )
}
