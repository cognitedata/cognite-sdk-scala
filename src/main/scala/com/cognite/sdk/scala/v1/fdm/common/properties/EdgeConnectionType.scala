package com.cognite.sdk.scala.v1.fdm.common.properties

import cats.implicits.catsSyntaxEq
import io.circe.{Decoder, Encoder}

sealed abstract class EdgeConnectionType extends Product with Serializable

object EdgeConnectionType {
  case object MultiEdgeConnection extends EdgeConnectionType
  case object SingleEdgeConnection extends EdgeConnectionType

  private val multiEdgeConnectionString = "multi_edge_connection"
  private val singleEdgeConnectionString = "single_edge_connection"

  implicit val edgeConnectionTypeDecoder: Decoder[EdgeConnectionType] = Decoder[String].emap {
    case s: String if s === multiEdgeConnectionString => Right(MultiEdgeConnection)
    case s: String if s === singleEdgeConnectionString => Right(SingleEdgeConnection)
    case other => Left(s"Invalid Connection type: $other")
  }

  implicit val edgeConnectionTypeEncoder: Encoder[EdgeConnectionType] =
    Encoder.instance[EdgeConnectionType] { p =>
      io.circe.Json.fromString(
        p match {
          case MultiEdgeConnection => multiEdgeConnectionString
          case SingleEdgeConnection => singleEdgeConnectionString
        }
      )
    }
}
