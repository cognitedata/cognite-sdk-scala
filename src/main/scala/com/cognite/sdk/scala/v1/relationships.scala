// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1

import java.time.Instant
import com.cognite.sdk.scala.common._

final case class Relationship(
    id: Long = 0,
    externalId: Option[String] = None,
    sourceExternalId: String,
    sourceType: String,
    targetExternalId: String,
    targetType: String,
    confidence: Option[Double] = None,
    startTime: Option[Instant] = None,
    endTime: Option[Instant] = None,
    createdTime: Instant = Instant.ofEpochMilli(0),
    lastUpdatedTime: Instant = Instant.ofEpochMilli(0),
    dataSetId: Option[Long] = None,
    labels: Option[Seq[CogniteExternalId]] = None
) extends WithId[Long]
    with WithExternalId
    with WithCreatedTime