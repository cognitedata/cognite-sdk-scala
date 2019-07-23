package com.cognite.sdk.scala.v1.resources

import com.cognite.sdk.scala.common._
import com.cognite.sdk.scala.v1._
import com.softwaremill.sttp._
import io.circe.generic.auto._

class TimeSeriesResource[F[_]](val requestSession: RequestSession[F])
    extends WithRequestSession[F]
    with Readable[TimeSeries, F]
    with RetrieveByIds[TimeSeries, F]
    with Create[TimeSeries, CreateTimeSeries, F]
    with DeleteByIdsV1[TimeSeries, CreateTimeSeries, F]
    with DeleteByExternalIdsV1[F]
    with Search[TimeSeries, TimeSeriesQuery, F]
    with Update[TimeSeries, TimeSeriesUpdate, F] {
  override val baseUri = uri"${requestSession.baseUri}/timeseries"

  override def readWithCursor(
      cursor: Option[String],
      limit: Option[Long]
  ): F[Response[ItemsWithCursor[TimeSeries]]] =
    Readable.readWithCursor(requestSession, baseUri, cursor, limit)

  override def retrieveByIds(ids: Seq[Long]): F[Response[Seq[TimeSeries]]] =
    RetrieveByIds.retrieveByIds(requestSession, baseUri, ids)

  override def createItems(items: Items[CreateTimeSeries]): F[Response[Seq[TimeSeries]]] =
    Create.createItems[F, TimeSeries, CreateTimeSeries](requestSession, baseUri, items)

  override def updateItems(items: Seq[TimeSeriesUpdate]): F[Response[Seq[TimeSeries]]] =
    Update.updateItems[F, TimeSeries, TimeSeriesUpdate](requestSession, baseUri, items)

  override def search(searchQuery: TimeSeriesQuery): F[Response[Seq[TimeSeries]]] =
    Search.search(requestSession, baseUri, searchQuery)
}
