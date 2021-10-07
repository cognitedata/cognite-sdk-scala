// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1

import java.time.Instant
import com.cognite.sdk.scala.common._

final case class Event(
    id: Long = 0,
    startTime: Option[Instant] = None,
    endTime: Option[Instant] = None,
    description: Option[String] = None,
    `type`: Option[String] = None,
    subtype: Option[String] = None,
    metadata: Option[Map[String, String]] = None,
    assetIds: Option[Seq[Long]] = None,
    source: Option[String] = None,
    externalId: Option[String] = None,
    createdTime: Instant = Instant.ofEpochMilli(0),
    lastUpdatedTime: Instant = Instant.ofEpochMilli(0),
    dataSetId: Option[Long] = None
) extends WithId[Long]
    with WithExternalId
    with WithCreatedTime
    with ToCreate[EventCreate]
    with ToUpdate[EventUpdate] {
  override def toCreate: EventCreate =
    EventCreate(
      startTime,
      endTime,
      description,
      `type`,
      subtype,
      metadata,
      assetIds,
      source,
      externalId,
      dataSetId
    )

  override def toUpdate: EventUpdate =
    EventUpdate(
      Setter.fromOption(startTime),
      Setter.fromOption(endTime),
      Setter.fromOption(description),
      Setter.fromOption(`type`),
      Setter.fromOption(subtype),
      NonNullableSetter.fromOption(metadata),
      NonNullableSetter.fromOption(assetIds),
      Setter.fromOption(source),
      Setter.fromOption(externalId),
      Setter.fromOption(dataSetId)
    )
}

final case class EventCreate(
    startTime: Option[Instant] = None,
    endTime: Option[Instant] = None,
    description: Option[String] = None,
    `type`: Option[String] = None,
    subtype: Option[String] = None,
    metadata: Option[Map[String, String]] = None,
    assetIds: Option[Seq[Long]] = None,
    source: Option[String] = None,
    externalId: Option[String] = None,
    dataSetId: Option[Long] = None
) extends WithExternalId

final case class EventUpdate(
    startTime: Option[Setter[Instant]] = None,
    endTime: Option[Setter[Instant]] = None,
    description: Option[Setter[String]] = None,
    `type`: Option[Setter[String]] = None,
    subtype: Option[Setter[String]] = None,
    metadata: Option[NonNullableSetter[Map[String, String]]] = None,
    assetIds: Option[NonNullableSetter[Seq[Long]]] = None,
    source: Option[Setter[String]] = None,
    externalId: Option[Setter[String]] = None,
    dataSetId: Option[Setter[Long]] = None
) extends WithSetExternalId

final case class EventsFilter(
    startTime: Option[TimeRange] = None,
    endTime: Option[TimeRange] = None,
    activeAtTime: Option[TimeRange] = None,
    metadata: Option[Map[String, String]] = None,
    assetIds: Option[Seq[Long]] = None,
    rootAssetIds: Option[Seq[CogniteId]] = None,
    source: Option[String] = None,
    `type`: Option[String] = None,
    subtype: Option[String] = None,
    createdTime: Option[TimeRange] = None,
    lastUpdatedTime: Option[TimeRange] = None,
    externalIdPrefix: Option[String] = None,
    dataSetIds: Option[Seq[CogniteId]] = None
)

final case class EventsSearch(
    description: Option[String] = None
)

final case class EventsQuery(
    filter: Option[EventsFilter] = None,
    search: Option[EventsSearch] = None,
    limit: Int = 100
) extends SearchQuery[EventsFilter, EventsSearch]
