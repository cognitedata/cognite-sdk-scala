package com.cognite.sdk.scala.v1.fdm.instances

import io.circe.{Decoder, Encoder}

import java.util.Locale

@deprecated("message", since = "0")
sealed abstract class SortDirection extends Product with Serializable


@deprecated("message", since = "0")
object SortDirection {
  case object Ascending extends SortDirection
  case object Descending extends SortDirection

  implicit val constraintTypeDecoder: Decoder[SortDirection] = Decoder[String].emap {
    case "ascending" => Right(Ascending)
    case "descending" => Right(Descending)
    case other => Left(s"Invalid Sort Direction: $other")
  }

  implicit val constraintTypeEncoder: Encoder[SortDirection] =
    Encoder.instance[SortDirection](p =>
      io.circe.Json.fromString(p.productPrefix.toLowerCase(Locale.US))
    )
}
