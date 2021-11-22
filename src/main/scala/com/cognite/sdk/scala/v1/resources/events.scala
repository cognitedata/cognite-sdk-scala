// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1.resources

import com.cognite.sdk.scala.common._
import com.cognite.sdk.scala.v1._
import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker
import sttp.client3._

class Events[F[_]](val requestSession: RequestSession[F])
    extends WithRequestSession[F]
    with RetrieveByIdsWithIgnoreUnknownIds[Event, F]
    with RetrieveByExternalIdsWithIgnoreUnknownIds[Event, F]
    with Create[Event, EventCreate, F]
    with DeleteByIdsWithIgnoreUnknownIds[F, Long]
    with DeleteByExternalIdsWithIgnoreUnknownIds[F]
    with Search[Event, EventsQuery, F]
    with UpdateById[Event, EventUpdate, F]
    with UpdateByExternalId[Event, EventUpdate, F] {
  import Events._
  override val baseUrl = uri"${requestSession.baseUrl}/events"

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

  override def deleteByIds(ids: Seq[Long]): F[Unit] = deleteByIds(ids, false)

  override def deleteByIds(ids: Seq[Long], ignoreUnknownIds: Boolean = false): F[Unit] =
    DeleteByIds.deleteByIdsWithIgnoreUnknownIds(requestSession, baseUrl, ids, ignoreUnknownIds)

  override def deleteByExternalIds(externalIds: Seq[String]): F[Unit] =
    deleteByExternalIds(externalIds, false)

  override def deleteByExternalIds(
      externalIds: Seq[String],
      ignoreUnknownIds: Boolean = false
  ): F[Unit] =
    DeleteByExternalIds.deleteByExternalIdsWithIgnoreUnknownIds(
      requestSession,
      baseUrl,
      externalIds,
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
      baseUrl,
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
  implicit val eventCodec: JsonValueCodec[Event] = JsonCodecMaker.make[Event]
  implicit val eventsItemsWithCursorCodec: JsonValueCodec[ItemsWithCursor[Event]] =
    JsonCodecMaker.make[ItemsWithCursor[Event]]
  implicit val eventsItemsCodec: JsonValueCodec[Items[Event]] =
    JsonCodecMaker.make[Items[Event]]
  implicit val createEventCodec: JsonValueCodec[EventCreate] = JsonCodecMaker.make[EventCreate]
  implicit val createEventsItemsCodec: JsonValueCodec[Items[EventCreate]] =
    JsonCodecMaker.make[Items[EventCreate]]
  implicit val eventUpdateCodec: JsonValueCodec[EventUpdate] =
    JsonCodecMaker.make[EventUpdate]
  implicit val updateEventsItemsCodec: JsonValueCodec[Items[EventUpdate]] =
    JsonCodecMaker.make[Items[EventUpdate]]
  implicit val eventsFilterCodec: JsonValueCodec[EventsFilter] =
    JsonCodecMaker.make[EventsFilter]
  implicit val eventsSearchCodec: JsonValueCodec[EventsSearch] =
    JsonCodecMaker.make[EventsSearch]
  implicit val eventsQueryCodec: JsonValueCodec[EventsQuery] =
    JsonCodecMaker.make[EventsQuery]
  implicit val eventsFilterRequestCodec: JsonValueCodec[FilterRequest[EventsFilter]] =
    JsonCodecMaker.make[FilterRequest[EventsFilter]]
}
