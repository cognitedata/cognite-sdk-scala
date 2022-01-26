// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1.resources

import com.cognite.sdk.scala.common._
import com.cognite.sdk.scala.v1._
import sttp.client3._
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

class TimeSeriesResource[F[_]](val requestSession: RequestSession[F])
    extends WithRequestSession[F]
    with Readable[TimeSeries, F]
    with RetrieveByIdsWithIgnoreUnknownIds[TimeSeries, F]
    with RetrieveByExternalIdsWithIgnoreUnknownIds[TimeSeries, F]
    with Create[TimeSeries, TimeSeriesCreate, F]
    with DeleteByIdsWithIgnoreUnknownIds[F, Long]
    with DeleteByExternalIdsWithIgnoreUnknownIds[F]
    with PartitionedFilter[TimeSeries, TimeSeriesFilter, F]
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
    DeleteByIds.deleteByCogniteIdsWithIgnoreUnknownIds(
      requestSession,
      baseUrl,
      ids.map(CogniteInternalId.apply),
      ignoreUnknownIds
    )

  override def deleteByExternalIds(externalIds: Seq[String]): F[Unit] =
    deleteByExternalIds(externalIds, false)

  override def deleteByExternalIds(
      externalIds: Seq[String],
      ignoreUnknownIds: Boolean = false
  ): F[Unit] =
    DeleteByIds.deleteByCogniteIdsWithIgnoreUnknownIds(
      requestSession,
      baseUrl,
      externalIds.map(CogniteExternalId.apply),
      ignoreUnknownIds
    )

  def deleteByCogniteIds(ids: Seq[CogniteId], ignoreUnknownIds: Boolean): F[Unit] =
    DeleteByIds.deleteByCogniteIdsWithIgnoreUnknownIds(
      requestSession,
      baseUrl,
      ids,
      ignoreUnknownIds
    )

  override private[sdk] def filterWithCursor(
      filter: TimeSeriesFilter,
      cursor: Option[String],
      limit: Option[Int],
      partition: Option[Partition],
      aggregatedProperties: Option[Seq[String]] = None
  ): F[ItemsWithCursor[TimeSeries]] =
    Filter.filterWithCursor(
      requestSession,
      baseUrl,
      filter,
      cursor,
      limit,
      partition,
      Constants.defaultBatchSize
    )

  override def search(searchQuery: TimeSeriesQuery): F[Seq[TimeSeries]] =
    Search.search(requestSession, baseUrl, searchQuery)
}

object TimeSeriesResource {
  implicit val timeSeriesDecoder: Decoder[TimeSeries] = deriveDecoder[TimeSeries]
  implicit val timeSeriesUpdateEncoder: Encoder[TimeSeriesUpdate] = deriveEncoder[TimeSeriesUpdate]
  implicit val timeSeriesItemsWithCursorDecoder: Decoder[ItemsWithCursor[TimeSeries]] =
    deriveDecoder[ItemsWithCursor[TimeSeries]]
  implicit val timeSeriesItemsDecoder: Decoder[Items[TimeSeries]] =
    deriveDecoder[Items[TimeSeries]]
  implicit val createTimeSeriesEncoder: Encoder[TimeSeriesCreate] = deriveEncoder[TimeSeriesCreate]
  implicit val createTimeSeriesItemsEncoder: Encoder[Items[TimeSeriesCreate]] =
    deriveEncoder[Items[TimeSeriesCreate]]
  implicit val timeSeriesFilterEncoder: Encoder[TimeSeriesSearchFilter] =
    deriveEncoder[TimeSeriesSearchFilter]
  implicit val timeSeriesSearchEncoder: Encoder[TimeSeriesSearch] =
    deriveEncoder[TimeSeriesSearch]
  implicit val timeSeriesQueryEncoder: Encoder[TimeSeriesQuery] =
    deriveEncoder[TimeSeriesQuery]
  implicit val assetsFilterEncoder: Encoder[TimeSeriesFilter] =
    deriveEncoder[TimeSeriesFilter]
  implicit val assetsFilterRequestEncoder: Encoder[FilterRequest[TimeSeriesFilter]] =
    deriveEncoder[FilterRequest[TimeSeriesFilter]]
}
