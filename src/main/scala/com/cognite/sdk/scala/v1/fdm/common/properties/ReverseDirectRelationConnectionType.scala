package com.cognite.sdk.scala.v1.fdm.common.properties

import cats.implicits.catsSyntaxEq
import io.circe.{Decoder, Encoder}

sealed abstract class ReverseDirectRelationConnectionType extends Product with Serializable

object ReverseDirectRelationConnectionType {
  case object MultiReverseDirectRelation extends ReverseDirectRelationConnectionType
  case object SingleReverseDirectRelation extends ReverseDirectRelationConnectionType

  private val multiReverseDirectRelationString = "multi_reverse_direct_relation"
  private val singleReverseDirectRelationString = "single_reverse_direct_relation"

  implicit val reverseDirectRelationConnectionTypeDecoder
      : Decoder[ReverseDirectRelationConnectionType] = Decoder[String].emap {
    case s: String if s === multiReverseDirectRelationString => Right(MultiReverseDirectRelation)
    case s: String if s === singleReverseDirectRelationString => Right(SingleReverseDirectRelation)
    case other => Left(s"Invalid Connection type: $other")
  }

  implicit val reverseDirectRelationConnectionTypeEncoder
      : Encoder[ReverseDirectRelationConnectionType] =
    Encoder.instance[ReverseDirectRelationConnectionType](p =>
      io.circe.Json.fromString(
        p match {
          case MultiReverseDirectRelation => multiReverseDirectRelationString
          case SingleReverseDirectRelation => singleReverseDirectRelationString
        }
      )
    )
}
