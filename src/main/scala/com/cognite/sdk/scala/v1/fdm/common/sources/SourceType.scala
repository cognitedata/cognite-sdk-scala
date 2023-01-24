package com.cognite.sdk.scala.v1.fdm.common.sources

import io.circe.{Decoder, Encoder}

import java.util.Locale

private[sdk] sealed abstract class SourceType extends Product with Serializable

object SourceType {
  private[sdk] case object Container extends SourceType

  private[sdk] case object View extends SourceType

  implicit val instantTypeDecoder: Decoder[SourceType] = Decoder[String].emap {
    case "container" => Right(Container)
    case "view" => Right(View)
    case other => Left(s"Invalid Source Reference Type: $other")
  }

  implicit val instantTypeEncoder: Encoder[SourceType] =
    Encoder.instance[SourceType](p =>
      io.circe.Json.fromString(p.productPrefix.toLowerCase(Locale.US))
    )
}
