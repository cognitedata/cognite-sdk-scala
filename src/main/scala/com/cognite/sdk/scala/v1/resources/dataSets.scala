package com.cognite.sdk.scala.v1.resources

import com.cognite.sdk.scala.common._
import com.cognite.sdk.scala.v1._
import com.softwaremill.sttp._
import io.circe.{Decoder, Encoder}
import io.circe.derivation.{deriveDecoder, deriveEncoder}

class DataSets[F[_]](val requestSession: RequestSession[F])
    extends WithRequestSession[F]
    with PartitionedReadable[DataSet, F]
    with Create[DataSet, DataSetCreate, F]
    with RetrieveByIds[DataSet, F]
    with RetrieveByExternalIds[DataSet, F]
    with PartitionedFilter[DataSet, DataSetFilter, F]
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
    Readable.readWithCursor(
      requestSession,
      baseUrl,
      cursor,
      limit,
      partition,
      Constants.defaultBatchSize
    )

  override def retrieveByIds(ids: Seq[Long]): F[Seq[DataSet]] =
    RetrieveByIds.retrieveByIds(requestSession, baseUrl, ids)

  override def retrieveByExternalIds(externalIds: Seq[String]): F[Seq[DataSet]] =
    RetrieveByExternalIds.retrieveByExternalIds(requestSession, baseUrl, externalIds)

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
  implicit val dataSetUpdateEncoder: Encoder[DataSetUpdate] = deriveEncoder[DataSetUpdate]
  implicit val dataSetFilterEncoder: Encoder[DataSetFilter] = deriveEncoder[DataSetFilter]
  implicit val dataSetFilterRequestEncoder: Encoder[FilterRequest[DataSetFilter]] = deriveEncoder[FilterRequest[DataSetFilter]]
  implicit val dataSetDecoder: Decoder[DataSet] = deriveDecoder[DataSet]
  implicit val dataSetItemsDecoder: Decoder[Items[DataSet]] = deriveDecoder[Items[DataSet]]
  implicit val dataSetItemsWithCursorDecoder: Decoder[ItemsWithCursor[DataSet]] = deriveDecoder[ItemsWithCursor[DataSet]]
  implicit val dataSetCreateEncoder: Encoder[DataSetCreate] = deriveEncoder[DataSetCreate]
  implicit val dataSetCreateItemsEncoder: Encoder[Items[DataSetCreate]] = deriveEncoder[Items[DataSetCreate]]
  implicit val dataSetListQueryEncoder: Encoder[DataSetQuery] = deriveEncoder[DataSetQuery]
}
