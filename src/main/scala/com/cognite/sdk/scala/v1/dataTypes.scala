package com.cognite.sdk.scala.v1

import java.time.Instant

import io.circe.{Decoder, Encoder}
import io.circe.derivation.deriveEncoder

final case class CogniteExternalId(externalId: String)

final case class TimeRange(min: Instant, max: Instant)
object TimeRange {
  implicit val instantEncoder: Encoder[Instant] = Encoder.encodeLong.contramap(_.toEpochMilli)
  implicit val instantDecoder: Decoder[Instant] = Decoder.decodeLong.map(Instant.ofEpochMilli)
  implicit val timeRangeEncoder: Encoder[TimeRange] = deriveEncoder
}
