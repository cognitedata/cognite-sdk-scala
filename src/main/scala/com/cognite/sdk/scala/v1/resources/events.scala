package com.cognite.sdk.scala.v1.resources

import java.time.Instant

import com.cognite.sdk.scala.common._
import com.cognite.sdk.scala.v1._
import com.softwaremill.sttp._
import io.circe.{Decoder, Encoder}
import io.circe.derivation.{deriveDecoder, deriveEncoder}

class Events[F[_]](val requestSession: RequestSession[F])
    extends WithRequestSession[F]
    with Readable[Event, F]
    with RetrieveByIds[Event, F]
    with Create[Event, CreateEvent, F]
    with DeleteByIds[F, Long]
    with DeleteByExternalIds[F]
    with Filter[Event, EventsFilter, F]
    with Search[Event, EventsQuery, F]
    with Update[Event, EventUpdate, F] {
  import Events._
  override val baseUri = uri"${requestSession.baseUri}/events"

  override def readWithCursor(
      cursor: Option[String],
      limit: Option[Long]
  ): F[ItemsWithCursor[Event]] =
    Readable.readWithCursor(requestSession, baseUri, cursor, limit)

  override def retrieveByIds(ids: Seq[Long]): F[Seq[Event]] =
    RetrieveByIds.retrieveByIds(requestSession, baseUri, ids)

  override def createItems(items: Items[CreateEvent]): F[Seq[Event]] =
    Create.createItems[F, Event, CreateEvent](requestSession, baseUri, items)

  override def update(items: Seq[EventUpdate]): F[Seq[Event]] =
    Update.update[F, Event, EventUpdate](requestSession, baseUri, items)

  override def deleteByIds(ids: Seq[Long]): F[Unit] =
    DeleteByIds.deleteByIds(requestSession, baseUri, ids)

  override def deleteByExternalIds(externalIds: Seq[String]): F[Unit] =
    DeleteByExternalIds.deleteByExternalIds(requestSession, baseUri, externalIds)

  override def filterWithCursor(
      filter: EventsFilter,
      cursor: Option[String],
      limit: Option[Long]
  ): F[ItemsWithCursor[Event]] =
    Filter.filterWithCursor(requestSession, baseUri, filter, cursor, limit)

  override def search(searchQuery: EventsQuery): F[Seq[Event]] =
    Search.search(requestSession, baseUri, searchQuery)
}

object Events {
  implicit val instantEncoder: Encoder[Instant] = Encoder.encodeLong.contramap(_.toEpochMilli)
  implicit val instantDecoder: Decoder[Instant] = Decoder.decodeLong.map(Instant.ofEpochMilli)

  implicit val eventDecoder: Decoder[Event] = deriveDecoder[Event]
  implicit val eventsItemsWithCursorDecoder: Decoder[ItemsWithCursor[Event]] =
    deriveDecoder[ItemsWithCursor[Event]]
  implicit val eventsItemsDecoder: Decoder[Items[Event]] =
    deriveDecoder[Items[Event]]
  implicit val createEventEncoder: Encoder[CreateEvent] = deriveEncoder[CreateEvent]
  implicit val createEventsItemsEncoder: Encoder[Items[CreateEvent]] =
    deriveEncoder[Items[CreateEvent]]
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
