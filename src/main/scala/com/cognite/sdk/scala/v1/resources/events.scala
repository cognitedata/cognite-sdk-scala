// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1.resources

import com.cognite.sdk.scala.common._
import com.cognite.sdk.scala.v1._
import sttp.client3._
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

class Events[F[_]](val requestSession: RequestSession[F])
    extends WithRequestSession[F]
    with PartitionedReadable[Event, F]
    with RetrieveByIdsWithIgnoreUnknownIds[Event, F]
    with RetrieveByExternalIdsWithIgnoreUnknownIds[Event, F]
    with Create[Event, EventCreate, F]
    with DeleteByCogniteIds[F]
    with PartitionedFilter[Event, EventsFilter, F]
    with Search[Event, EventsQuery, F]
    with UpdateById[Event, EventUpdate, F]
    with UpdateByExternalId[Event, EventUpdate, F] {
  import Events._
  override val baseUrl = uri"${requestSession.baseUrl}/events"

  override private[sdk] def readWithCursor(
      cursor: Option[String],
      limit: Option[Int],
      partition: Option[Partition]
  ): F[ItemsWithCursor[Event]] =
    Readable.readWithCursor(
      requestSession,
      baseUrl,
      cursor,
      limit,
      partition,
      Constants.defaultBatchSize
    )

  override def retrieveByIds(
      ids: Seq[Long],
      ignoreUnknownIds: Boolean
  ): F[Seq[Event]] =
    RetrieveByIdsWithIgnoreUnknownIds.retrieveByIds(
      requestSession,
      baseUrl,
      ids,
      ignoreUnknownIds
    )

  override def retrieveByExternalIds(
      externalIds: Seq[String],
      ignoreUnknownIds: Boolean
  ): F[Seq[Event]] =
    RetrieveByExternalIdsWithIgnoreUnknownIds.retrieveByExternalIds(
      requestSession,
      baseUrl,
      externalIds,
      ignoreUnknownIds
    )

  override def createItems(items: Items[EventCreate]): F[Seq[Event]] =
    Create.createItems[F, Event, EventCreate](requestSession, baseUrl, items)

  override def updateById(items: Map[Long, EventUpdate]): F[Seq[Event]] =
    UpdateById.updateById[F, Event, EventUpdate](requestSession, baseUrl, items)

  override def updateByExternalId(items: Map[String, EventUpdate]): F[Seq[Event]] =
    UpdateByExternalId.updateByExternalId[F, Event, EventUpdate](requestSession, baseUrl, items)

  override def delete(ids: Seq[CogniteId], ignoreUnknownIds: Boolean = false): F[Unit] =
    DeleteByCogniteIds.deleteWithIgnoreUnknownIds(
      requestSession,
      baseUrl,
      ids,
      ignoreUnknownIds
    )

  private[sdk] def filterWithCursor(
      filter: EventsFilter,
      cursor: Option[String],
      limit: Option[Int],
      partition: Option[Partition],
      aggregatedProperties: Option[Seq[String]] = None
  ): F[ItemsWithCursor[Event]] =
    Filter.filterWithCursor(
      requestSession,
      uri"$baseUrl/list",
      filter,
      cursor,
      limit,
      partition,
      Constants.defaultBatchSize
    )

  override def search(searchQuery: EventsQuery): F[Seq[Event]] =
    Search.search(requestSession, baseUrl, searchQuery)
}

object Events {
  implicit val eventDecoder: Decoder[Event] = deriveDecoder[Event]
  implicit val eventsItemsWithCursorDecoder: Decoder[ItemsWithCursor[Event]] =
    deriveDecoder[ItemsWithCursor[Event]]
  implicit val eventsItemsDecoder: Decoder[Items[Event]] =
    deriveDecoder[Items[Event]]
  implicit val createEventEncoder: Encoder[EventCreate] = deriveEncoder[EventCreate]
  implicit val createEventsItemsEncoder: Encoder[Items[EventCreate]] =
    deriveEncoder[Items[EventCreate]]
  implicit val eventUpdateEncoder: Encoder[EventUpdate] =
    deriveEncoder[EventUpdate]
  implicit val updateEventsItemsEncoder: Encoder[Items[EventUpdate]] =
    deriveEncoder[Items[EventUpdate]]
  implicit val eventsFilterEncoder: Encoder[EventsFilter] =
    deriveEncoder[EventsFilter]
  implicit val eventsSearchEncoder: Encoder[EventsSearch] =
    deriveEncoder[EventsSearch]
  implicit val eventsQueryEncoder: Encoder[EventsQuery] =
    deriveEncoder[EventsQuery]
  implicit val eventsFilterRequestEncoder: Encoder[FilterRequest[EventsFilter]] =
    deriveEncoder[FilterRequest[EventsFilter]]
}
