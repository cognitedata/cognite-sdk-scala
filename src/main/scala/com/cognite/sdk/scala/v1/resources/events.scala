package com.cognite.sdk.scala.v1.resources

import com.cognite.sdk.scala.common._
import com.cognite.sdk.scala.v1.{
  CreateEvent,
  Event,
  EventUpdate,
  EventsFilter,
  EventsQuery,
  RequestSession
}
import com.softwaremill.sttp._
import io.circe.generic.auto._

class Events[F[_]](val requestSession: RequestSession[F])
    extends WithRequestSession[F]
    with Readable[Event, F]
    with RetrieveByIds[Event, F]
    with Create[Event, CreateEvent, F]
    with DeleteByIdsV1[Event, CreateEvent, F]
    with DeleteByExternalIdsV1[F]
    with Filter[Event, EventsFilter, F]
    with Search[Event, EventsQuery, F]
    with Update[Event, EventUpdate, F] {
  override val baseUri = uri"${requestSession.baseUri}/events"

  override def readWithCursor(
      cursor: Option[String],
      limit: Option[Long]
  ): F[Response[ItemsWithCursor[Event]]] =
    Readable.readWithCursor(requestSession, baseUri, cursor, limit)

  override def retrieveByIds(ids: Seq[Long]): F[Response[Seq[Event]]] =
    RetrieveByIds.retrieveByIds(requestSession, baseUri, ids)

  override def createItems(items: Items[CreateEvent]): F[Response[Seq[Event]]] =
    Create.createItems[F, Event, CreateEvent](requestSession, baseUri, items)

  override def updateItems(items: Seq[EventUpdate]): F[Response[Seq[Event]]] =
    Update.updateItems[F, Event, EventUpdate](requestSession, baseUri, items)

  override def filterWithCursor(
      filter: EventsFilter,
      cursor: Option[String],
      limit: Option[Long]
  ): F[Response[ItemsWithCursor[Event]]] =
    Filter.filterWithCursor(requestSession, baseUri, filter, cursor, limit)

  override def search(searchQuery: EventsQuery): F[Response[Seq[Event]]] =
    Search.search(requestSession, baseUri, searchQuery)
}
