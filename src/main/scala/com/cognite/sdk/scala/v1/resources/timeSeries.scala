package com.cognite.sdk.scala.v1.resources

import java.time.Instant

import com.cognite.sdk.scala.common._
import com.cognite.sdk.scala.v1._
import com.softwaremill.sttp._
import io.circe.derivation.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

class TimeSeriesResource[F[_]](val requestSession: RequestSession[F])
    extends WithRequestSession[F]
    with Readable[TimeSeries, F]
    with RetrieveByIds[TimeSeries, F]
    with Create[TimeSeries, CreateTimeSeries, F]
    with DeleteByIds[F, Long]
    with DeleteByExternalIds[F]
    with Search[TimeSeries, TimeSeriesQuery, F]
    with Update[TimeSeries, TimeSeriesUpdate, F] {
  import TimeSeriesResource._
  override val baseUri = uri"${requestSession.baseUri}/timeseries"

  override def readWithCursor(
      cursor: Option[String],
      limit: Option[Long]
  ): F[ItemsWithCursor[TimeSeries]] =
    Readable.readWithCursor(requestSession, baseUri, cursor, limit)

  override def retrieveByIds(ids: Seq[Long]): F[Seq[TimeSeries]] =
    RetrieveByIds.retrieveByIds(requestSession, baseUri, ids)

  override def createItems(items: Items[CreateTimeSeries]): F[Seq[TimeSeries]] =
    Create.createItems[F, TimeSeries, CreateTimeSeries](requestSession, baseUri, items)

  override def update(items: Seq[TimeSeriesUpdate]): F[Seq[TimeSeries]] =
    Update.update[F, TimeSeries, TimeSeriesUpdate](requestSession, baseUri, items)

  override def deleteByIds(ids: Seq[Long]): F[Unit] =
    DeleteByIds.deleteByIds(requestSession, baseUri, ids)

  override def deleteByExternalIds(externalIds: Seq[String]): F[Unit] =
    DeleteByExternalIds.deleteByExternalIds(requestSession, baseUri, externalIds)

  override def search(searchQuery: TimeSeriesQuery): F[Seq[TimeSeries]] =
    Search.search(requestSession, baseUri, searchQuery)
}

object TimeSeriesResource {
  implicit val instantEncoder: Encoder[Instant] = Encoder.encodeLong.contramap(_.toEpochMilli)
  implicit val instantDecoder: Decoder[Instant] = Decoder.decodeLong.map(Instant.ofEpochMilli)

  implicit val timeSeriesDecoder: Decoder[TimeSeries] = deriveDecoder[TimeSeries]
  implicit val timeSeriesUpdateEncoder: Encoder[TimeSeriesUpdate] = deriveEncoder[TimeSeriesUpdate]
  implicit val timeSeriesItemsWithCursorDecoder: Decoder[ItemsWithCursor[TimeSeries]] =
    deriveDecoder[ItemsWithCursor[TimeSeries]]
  implicit val timeSeriesItemsDecoder: Decoder[Items[TimeSeries]] =
    deriveDecoder[Items[TimeSeries]]
  implicit val createTimeSeriesEncoder: Encoder[CreateTimeSeries] = deriveEncoder[CreateTimeSeries]
  implicit val createTimeSeriesItemsEncoder: Encoder[Items[CreateTimeSeries]] =
    deriveEncoder[Items[CreateTimeSeries]]
  implicit val timeSeriesFilterEncoder: Encoder[TimeSeriesFilter] =
    deriveEncoder[TimeSeriesFilter]
  implicit val timeSeriesSearchEncoder: Encoder[TimeSeriesSearch] =
    deriveEncoder[TimeSeriesSearch]
  implicit val timeSeriesQueryEncoder: Encoder[TimeSeriesQuery] =
    deriveEncoder[TimeSeriesQuery]
}
