package com.cognite.sdk.scala.v1.resources

import java.time.Instant

import com.cognite.sdk.scala.common._
import com.cognite.sdk.scala.v1._
import com.softwaremill.sttp._
import io.circe.{Decoder, Encoder}
import io.circe.derivation.{deriveDecoder, deriveEncoder}

class Events[F[_]](val requestSession: RequestSession[F])
    extends WithRequestSession[F]
    with PartitionedReadable[Event, F]
    with RetrieveByIds[Event, F]
    with RetrieveByExternalIds[Event, F]
    with Create[Event, EventCreate, F]
    with DeleteByIds[F, Long]
    with DeleteByExternalIds[F]
    with PartitionedFilter[Event, EventsFilter, F]
    with Search[Event, EventsQuery, F]
    with Update[Event, EventUpdate, F] {
  import Events._
  override val baseUri = uri"${requestSession.baseUri}/events"

  override private[sdk] def readWithCursor(
      cursor: Option[String],
      limit: Option[Int],
      partition: Option[Partition]
  ): F[ItemsWithCursor[Event]] =
    Readable.readWithCursor(
      requestSession,
      baseUri,
      cursor,
      limit,
      partition,
      Constants.defaultBatchSize
    )

  override def retrieveByIds(ids: Seq[Long]): F[Seq[Event]] =
    RetrieveByIds.retrieveByIds(requestSession, baseUri, ids)

  override def retrieveByExternalIds(externalIds: Seq[String]): F[Seq[Event]] =
    RetrieveByExternalIds.retrieveByExternalIds(requestSession, baseUri, externalIds)

  override def createItems(items: Items[EventCreate]): F[Seq[Event]] =
    Create.createItems[F, Event, EventCreate](requestSession, baseUri, items)

  override def update(items: Seq[EventUpdate]): F[Seq[Event]] =
    Update.update[F, Event, EventUpdate](requestSession, baseUri, items)

  override def deleteByIds(ids: Seq[Long]): F[Unit] =
    DeleteByIds.deleteByIds(requestSession, baseUri, ids)

  override def deleteByExternalIds(externalIds: Seq[String]): F[Unit] =
    DeleteByExternalIds.deleteByExternalIds(requestSession, baseUri, externalIds)

  private[sdk] def filterWithCursor(
      filter: EventsFilter,
      cursor: Option[String],
      limit: Option[Int],
      partition: Option[Partition]
  ): F[ItemsWithCursor[Event]] =
    Filter.filterWithCursor(requestSession, baseUri, filter, cursor, limit, partition)

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
