package com.cognite.sdk.scala.v1

import com.cognite.sdk.scala.common.{NonNullableSetter, Setter, SearchQuery, TimeRange, WithId}

final case class Event(
    id: Long = 0,
    startTime: Option[Long] = None,
    endTime: Option[Long] = None,
    description: Option[String] = None,
    `type`: Option[String] = None,
    subtype: Option[String] = None,
    metadata: Option[Map[String, String]] = None,
    assetIds: Option[Seq[Long]] = None,
    source: Option[String] = None,
    externalId: Option[String] = None,
    createdTime: Long = 0,
    lastUpdatedTime: Long = 0
) extends WithId[Long]

final case class CreateEvent(
    startTime: Option[Long] = None,
    endTime: Option[Long] = None,
    description: Option[String] = None,
    `type`: Option[String] = None,
    subtype: Option[String] = None,
    metadata: Option[Map[String, String]] = None,
    assetIds: Option[Seq[Long]] = None,
    source: Option[String] = None,
    externalId: Option[String] = None
)

final case class EventUpdate(
    id: Long = 0,
    startTime: Option[Setter[Long]] = None,
    endTime: Option[Setter[Long]] = None,
    description: Option[Setter[String]] = None,
    `type`: Option[Setter[String]] = None,
    subtype: Option[Setter[String]] = None,
    metadata: Option[NonNullableSetter[Map[String, String]]] = None,
    assetIds: Option[NonNullableSetter[Seq[Long]]] = None,
    source: Option[Setter[String]] = None,
    externalId: Option[Setter[String]] = None
) extends WithId[Long]

final case class EventsFilter(
    startTime: Option[TimeRange] = None,
    endTime: Option[TimeRange] = None,
    metadata: Option[Map[String, String]] = None,
    assetIds: Option[Seq[Long]] = None,
    source: Option[String] = None,
    `type`: Option[String] = None,
    subType: Option[String] = None,
    createdTime: Option[TimeRange] = None,
    lastUpdatedTime: Option[TimeRange] = None,
    externalIdPrefix: Option[String] = None
)

final case class EventsSearch(
    description: Option[String] = None
)

final case class EventsQuery(
    filter: Option[EventsFilter] = None,
    search: Option[EventsSearch] = None,
    limit: Int = 100
) extends SearchQuery[EventsFilter, EventsSearch]
