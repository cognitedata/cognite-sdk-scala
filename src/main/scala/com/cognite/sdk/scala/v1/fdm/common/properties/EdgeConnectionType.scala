package com.cognite.sdk.scala.v1.fdm.common.properties

import io.circe.{Decoder, Encoder}

import java.util.Locale

sealed abstract class EdgeConnectionType extends Product with Serializable

object EdgeConnectionType {
  case object MultiEdgeConnection extends EdgeConnectionType
  case object SingleEdgeConnection extends EdgeConnectionType

  implicit val edgeConnectionTypeDecoder: Decoder[EdgeConnectionType] = Decoder[String].emap {
    case "multi_edge_connection" => Right(MultiEdgeConnection)
    case "single_edge_connection" => Right(SingleEdgeConnection)
    case other => Left(s"Invalid Connection type: $other")
  }

  implicit val edgeConnectionTypeEncoder: Encoder[EdgeConnectionType] =
    Encoder.instance[EdgeConnectionType](p =>
      io.circe.Json.fromString(p.productPrefix.toLowerCase(Locale.US))
    )
}
