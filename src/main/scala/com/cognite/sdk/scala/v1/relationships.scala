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
    startTime: Option[Instant] = None,
    endTime: Option[Instant] = None,
    confidence: Option[Double] = None,
    dataSetId: Option[Long] = None,
    labels: Option[Seq[CogniteExternalId]] = None,
    createdTime: Instant = Instant.ofEpochMilli(0),
    lastUpdatedTime: Instant = Instant.ofEpochMilli(0)
) extends WithRequiredExternalId
    with WithCreatedTime
    with ToCreate[RelationshipCreate] {
  override def toCreate: RelationshipCreate =
    RelationshipCreate(
      externalId,
      sourceExternalId,
      sourceType,
      targetExternalId,
      targetType,
      startTime,
      endTime,
      confidence,
      dataSetId,
      labels
    )
}

final case class RelationshipCreate(
    externalId: String,
    sourceExternalId: String,
    sourceType: String,
    targetExternalId: String,
    targetType: String,
    startTime: Option[Instant] = None,
    endTime: Option[Instant] = None,
    confidence: Option[Double] = None,
    dataSetId: Option[Long] = None,
    labels: Option[Seq[CogniteExternalId]] = None
) extends WithRequiredExternalId

final case class RelationshipsFilter(
    sourceExternalIds: Option[Seq[String]] = None,
    sourceTypes: Option[Seq[String]] = None,
    targetExternalIds: Option[Seq[String]] = None,
    targetTypes: Option[Seq[String]] = None,
    dataSetIds: Option[Seq[CogniteId]] = None,
    startTime: Option[TimeRange] = None,
    endTime: Option[TimeRange] = None,
    confidence: Option[ConfidenceRange] = None,
    lastUpdatedTime: Option[TimeRange] = None,
    createdTime: Option[TimeRange] = None,
    activeAtTime: Option[TimeRange] = None,
    labels: Option[LabelContainsFilter] = None
)
