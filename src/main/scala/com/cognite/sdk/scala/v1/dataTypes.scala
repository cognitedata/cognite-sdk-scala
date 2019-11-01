package com.cognite.sdk.scala.v1

import java.time.Instant

sealed trait CogniteId

final case class CogniteExternalId(externalId: String) extends CogniteId
final case class CogniteInternalId(id: Long) extends CogniteId

final case class TimeRange(min: Instant, max: Instant)
