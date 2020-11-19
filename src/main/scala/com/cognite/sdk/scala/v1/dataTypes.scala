// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1

import java.time.Instant

sealed trait CogniteId

final case class CogniteExternalId(externalId: String) extends CogniteId
final case class CogniteInternalId(id: Long) extends CogniteId

final case class TimeRange(min: Instant, max: Instant)
final case class ConfidenceRange(min: Double, max: Double)

final case class LabelContainsAny(containsAny: CogniteExternalId)
final case class LabelContainsAll(containsAll: CogniteExternalId)