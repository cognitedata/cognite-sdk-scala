// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1

import java.time.Instant
import com.cognite.sdk.scala.common._

final case class Relationship(
    externalId: String,
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
) extends WithRequiredExternalId
    with WithCreatedTime

final case class RelationshipCreate(
    externalId: String,
    sourceExternalId: String,
    sourceType: String,
    targetExternalId: String,
    targetType: String,
    confidence: Option[Double] = None,
    startTime: Option[Instant] = None,
    endTime: Option[Instant] = None,
    dataSetId: Option[Long] = None,
    labels: Option[Seq[CogniteExternalId]] = None
) extends WithRequiredExternalId

final case class RelationshipsFilter(
   sourceExternalIds: Option[Seq[String]] = None,
   sourceType: Option[Seq[String]] = None,
   targetExternalId: Option[Seq[String]] = None,
   targetType: Option[Seq[String]] = None,
   dataSetIds: Option[Seq[CogniteId]] = None,
   startTime: Option[TimeRange] = None,
   endTime: Option[TimeRange] = None,
   confidence: Option[ConfidenceRange] = None,
   createdTime: Option[TimeRange] = None,
   lastUpdatedTime: Option[TimeRange] = None,
   activeAtTime: Option[TimeRange] = None,
   labels: Option[LabelContainsFilter] = None
)