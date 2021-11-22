// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1

import java.time.Instant
import com.cognite.sdk.scala.common._

final case class Relationship(
    externalId: Some[String],
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
    with ToCreate[RelationshipCreate]
    with ToUpdate[RelationshipUpdate] {
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

  override def toUpdate: RelationshipUpdate =
    RelationshipUpdate(
      Setter.fromAny(externalId.get),
      Some(NonNullableSetter.fromAny(sourceExternalId)),
      Some(NonNullableSetter.fromAny(sourceType)),
      Some(NonNullableSetter.fromAny(targetExternalId)),
      Some(NonNullableSetter.fromAny(targetType)),
      Setter.fromOption(startTime),
      Setter.fromOption(endTime),
      Setter.fromOption(confidence),
      Setter.fromOption(dataSetId),
      NonNullableSetter.fromOption(labels)
    )
}

final case class RelationshipCreate(
    externalId: Some[String],
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

final case class RelationshipUpdate(
    externalId: Option[Setter[String]] = None,
    sourceExternalId: Option[NonNullableSetter[String]] = None,
    sourceType: Option[NonNullableSetter[String]] = None,
    targetExternalId: Option[NonNullableSetter[String]] = None,
    targetType: Option[NonNullableSetter[String]] = None,
    startTime: Option[Setter[Instant]] = None,
    endTime: Option[Setter[Instant]] = None,
    confidence: Option[Setter[Double]] = None,
    dataSetId: Option[Setter[Long]] = None,
    labels: Option[NonNullableSetter[Seq[CogniteExternalId]]] = None
) extends WithSetExternalId

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
