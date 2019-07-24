package com.cognite.sdk.scala.v1

import io.circe.Encoder
import io.circe.derivation.deriveEncoder

final case class CogniteExternalId(id: String)

final case class TimeRange(min: Long, max: Long)
object TimeRange {
  implicit val timeRangeEncoder: Encoder[TimeRange] = deriveEncoder
}
