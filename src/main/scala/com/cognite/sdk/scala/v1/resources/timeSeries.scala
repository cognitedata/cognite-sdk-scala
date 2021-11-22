// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1.resources

import com.cognite.sdk.scala.common._
import com.cognite.sdk.scala.v1._
import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker
import sttp.client3._

class TimeSeriesResource[F[_]](val requestSession: RequestSession[F])
    extends WithRequestSession[F]
    with Readable[TimeSeries, F]
    with RetrieveByIdsWithIgnoreUnknownIds[TimeSeries, F]
    with RetrieveByExternalIdsWithIgnoreUnknownIds[TimeSeries, F]
    with Create[TimeSeries, TimeSeriesCreate, F]
    with DeleteByIdsWithIgnoreUnknownIds[F, Long]
    with DeleteByExternalIdsWithIgnoreUnknownIds[F]
    with Search[TimeSeries, TimeSeriesQuery, F]
    with UpdateById[TimeSeries, TimeSeriesUpdate, F]
    with UpdateByExternalId[TimeSeries, TimeSeriesUpdate, F] {
  import TimeSeriesResource._
  override val baseUrl = uri"${requestSession.baseUrl}/timeseries"

  override private[sdk] def readWithCursor(
      cursor: Option[String],
      limit: Option[Int],
      partition: Option[Partition]
  ): F[ItemsWithCursor[TimeSeries]] =
    Readable.readWithCursor(
      requestSession,
      baseUrl,
      cursor,
      limit,
      None,
      Constants.defaultBatchSize
    )

  override def retrieveByIds(
      ids: Seq[Long],
      ignoreUnknownIds: Boolean
  ): F[Seq[TimeSeries]] =
    RetrieveByIdsWithIgnoreUnknownIds.retrieveByIds(
      requestSession,
      baseUrl,
      ids,
      ignoreUnknownIds
    )

  override def retrieveByExternalIds(
      externalIds: Seq[String],
      ignoreUnknownIds: Boolean
  ): F[Seq[TimeSeries]] =
    RetrieveByExternalIdsWithIgnoreUnknownIds.retrieveByExternalIds(
      requestSession,
      baseUrl,
      externalIds,
      ignoreUnknownIds
    )

  override def createItems(items: Items[TimeSeriesCreate]): F[Seq[TimeSeries]] =
    Create.createItems[F, TimeSeries, TimeSeriesCreate](requestSession, baseUrl, items)

  override def updateById(items: Map[Long, TimeSeriesUpdate]): F[Seq[TimeSeries]] =
    UpdateById.updateById[F, TimeSeries, TimeSeriesUpdate](requestSession, baseUrl, items)

  override def updateByExternalId(items: Map[String, TimeSeriesUpdate]): F[Seq[TimeSeries]] =
    UpdateByExternalId.updateByExternalId[F, TimeSeries, TimeSeriesUpdate](
      requestSession,
      baseUrl,
      items
    )

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

  override def search(searchQuery: TimeSeriesQuery): F[Seq[TimeSeries]] =
    Search.search(requestSession, baseUrl, searchQuery)
}

object TimeSeriesResource {
  implicit val timeSeriesCodec: JsonValueCodec[TimeSeries] = JsonCodecMaker.make[TimeSeries]
  implicit val timeSeriesUpdateCodec: JsonValueCodec[TimeSeriesUpdate] = JsonCodecMaker.make[TimeSeriesUpdate]
  implicit val timeSeriesItemsWithCursorCodec: JsonValueCodec[ItemsWithCursor[TimeSeries]] =
    JsonCodecMaker.make[ItemsWithCursor[TimeSeries]]
  implicit val timeSeriesItemsCodec: JsonValueCodec[Items[TimeSeries]] =
    JsonCodecMaker.make[Items[TimeSeries]]
  implicit val createTimeSeriesCodec: JsonValueCodec[TimeSeriesCreate] = JsonCodecMaker.make[TimeSeriesCreate]
  implicit val createTimeSeriesItemsCodec: JsonValueCodec[Items[TimeSeriesCreate]] =
    JsonCodecMaker.make[Items[TimeSeriesCreate]]
  implicit val timeSeriesFilterCodec: JsonValueCodec[TimeSeriesSearchFilter] =
    JsonCodecMaker.make[TimeSeriesSearchFilter]
  implicit val timeSeriesSearchCodec: JsonValueCodec[TimeSeriesSearch] =
    JsonCodecMaker.make[TimeSeriesSearch]
  implicit val timeSeriesQueryCodec: JsonValueCodec[TimeSeriesQuery] =
    JsonCodecMaker.make[TimeSeriesQuery]
  implicit val assetsFilterCodec: JsonValueCodec[TimeSeriesFilter] =
    JsonCodecMaker.make[TimeSeriesFilter]
  implicit val assetsFilterRequestCodec: JsonValueCodec[FilterRequest[TimeSeriesFilter]] =
    JsonCodecMaker.make[FilterRequest[TimeSeriesFilter]]
}
