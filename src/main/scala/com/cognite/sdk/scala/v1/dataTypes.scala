// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1

import java.time.Instant

sealed trait CogniteId

final case class CogniteExternalId(externalId: String) extends CogniteId
final case class CogniteInternalId(id: Long) extends CogniteId

// min and max needs to optional, since one of them can be provided alone.
final case class TimeRange(min: Option[Instant], max: Option[Instant])
final case class ConfidenceRange(min: Option[Double], max: Option[Double])

final case class LabelContainsAnyAll(containsAny: Option[Seq[CogniteExternalId]] = None, containsAll: Option[Seq[CogniteExternalId]] = None)