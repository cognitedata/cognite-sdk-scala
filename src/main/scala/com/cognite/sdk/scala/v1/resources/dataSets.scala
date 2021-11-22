// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1.resources

import com.cognite.sdk.scala.common._
import com.cognite.sdk.scala.v1._
import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker
import sttp.client3._

class DataSets[F[_]](val requestSession: RequestSession[F])
    extends WithRequestSession[F]
    with Readable[DataSet, F]
    with Create[DataSet, DataSetCreate, F]
    with RetrieveByIdsWithIgnoreUnknownIds[DataSet, F]
    with RetrieveByExternalIdsWithIgnoreUnknownIds[DataSet, F]
    with Filter[DataSet, DataSetFilter, F]
    with Search[DataSet, DataSetQuery, F]
    with UpdateById[DataSet, DataSetUpdate, F]
    with UpdateByExternalId[DataSet, DataSetUpdate, F] {
  import DataSets._
  override val baseUrl = uri"${requestSession.baseUrl}/datasets"

  override private[sdk] def readWithCursor(
      cursor: Option[String],
      limit: Option[Int],
      partition: Option[Partition]
  ): F[ItemsWithCursor[DataSet]] =
    filterWithCursor(DataSetFilter(), cursor, limit, None, None)

  override def retrieveByIds(
      ids: Seq[Long],
      ignoreUnknownIds: Boolean
  ): F[Seq[DataSet]] =
    RetrieveByIdsWithIgnoreUnknownIds.retrieveByIds(
      requestSession,
      baseUrl,
      ids,
      ignoreUnknownIds
    )

  override def retrieveByExternalIds(
      externalIds: Seq[String],
      ignoreUnknownIds: Boolean
  ): F[Seq[DataSet]] =
    RetrieveByExternalIdsWithIgnoreUnknownIds.retrieveByExternalIds(
      requestSession,
      baseUrl,
      externalIds,
      ignoreUnknownIds
    )

  override def createItems(items: Items[DataSetCreate]): F[Seq[DataSet]] =
    Create.createItems[F, DataSet, DataSetCreate](requestSession, baseUrl, items)

  override def updateById(items: Map[Long, DataSetUpdate]): F[Seq[DataSet]] =
    UpdateById.updateById[F, DataSet, DataSetUpdate](requestSession, baseUrl, items)

  override def updateByExternalId(items: Map[String, DataSetUpdate]): F[Seq[DataSet]] =
    UpdateByExternalId.updateByExternalId[F, DataSet, DataSetUpdate](
      requestSession,
      baseUrl,
      items
    )

  private[sdk] def filterWithCursor(
      filter: DataSetFilter,
      cursor: Option[String],
      limit: Option[Int],
      partition: Option[Partition],
      aggregatedProperties: Option[Seq[String]] = None
  ): F[ItemsWithCursor[DataSet]] =
    Filter.filterWithCursor(
      requestSession,
      baseUrl,
      filter,
      cursor,
      limit,
      partition = None,
      Constants.defaultBatchSize
    )

  override def search(searchQuery: DataSetQuery): F[Seq[DataSet]] =
    Search.search(requestSession, baseUrl, searchQuery)
}

object DataSets {
  implicit val dataSetCodec: JsonValueCodec[DataSet] = JsonCodecMaker.make
  implicit val dataSetItemsWithCursorCodec: JsonValueCodec[ItemsWithCursor[DataSet]] = JsonCodecMaker.make
  implicit val dataSetItemsCodec: JsonValueCodec[Items[DataSet]] = JsonCodecMaker.make
  implicit val dataSetCreateCodec: JsonValueCodec[DataSetCreate] = JsonCodecMaker.make
  implicit val dataSetCreateItemsCodec: JsonValueCodec[Items[DataSetCreate]] = JsonCodecMaker.make
  implicit val dataSetUpdateCodec: JsonValueCodec[DataSetUpdate] = JsonCodecMaker.make
  implicit val dataSetUpdateItemsCodec: JsonValueCodec[Items[DataSetUpdate]] = JsonCodecMaker.make
  implicit val dataSetFilterCodec: JsonValueCodec[DataSetFilter] = JsonCodecMaker.make
  implicit val dataSetFilterRequestCodec: JsonValueCodec[FilterRequest[DataSetFilter]] = JsonCodecMaker.make
  implicit val dataSetListQueryCodec: JsonValueCodec[DataSetQuery] = JsonCodecMaker.make
}
