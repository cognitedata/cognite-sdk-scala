package com.cognite.sdk.scala.v1.instances

import io.circe.{Decoder, Encoder}

import java.util.Locale

sealed abstract class InstanceType extends Product with Serializable

object InstanceType {
  case object Node extends InstanceType

  case object Edge extends InstanceType

  implicit val instantTypeDecoder: Decoder[InstanceType] = Decoder[String].emap {
    case "node" => Right(Node)
    case "edge" => Right(Node)
    case other => Left(s"Invalid Instance Type: $other")
  }

  implicit val instantTypeEncoder: Encoder[InstanceType] =
    Encoder.instance[InstanceType](p =>
      io.circe.Json.fromString(p.productPrefix.toLowerCase(Locale.US))
    )
}
