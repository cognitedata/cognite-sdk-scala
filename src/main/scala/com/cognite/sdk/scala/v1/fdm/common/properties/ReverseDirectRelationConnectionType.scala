package com.cognite.sdk.scala.v1.fdm.common.properties

import io.circe.{Decoder, Encoder}

import java.util.Locale

sealed abstract class ReverseDirectRelationConnectionType extends Product with Serializable

object ReverseDirectRelationConnectionType {
  case object MultiReverseDirectRelation extends ReverseDirectRelationConnectionType
  case object SingleReverseDirectRelation extends ReverseDirectRelationConnectionType

  implicit val reverseDirectRelationConnectionTypeDecoder
      : Decoder[ReverseDirectRelationConnectionType] = Decoder[String].emap {
    case "multi_reverse_direct_relation" => Right(MultiReverseDirectRelation)
    case "single_reverse_direct_relation" => Right(SingleReverseDirectRelation)
    case other => Left(s"Invalid Connection direction: $other")
  }

  implicit val reverseDirectRelationConnectionTypeEncoder
      : Encoder[ReverseDirectRelationConnectionType] =
    Encoder.instance[ReverseDirectRelationConnectionType](p =>
      io.circe.Json.fromString(
        p.productPrefix.replaceAll("([a-z])([A-Z]+)", "$1_$2").toLowerCase(Locale.US)
      )
    )
}
