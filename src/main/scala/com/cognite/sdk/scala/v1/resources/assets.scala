// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1.resources

import com.cognite.sdk.scala.common._
import com.cognite.sdk.scala.v1._
import sttp.client3._
import sttp.client3.circe._
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

class Assets[F[_]](val requestSession: RequestSession[F])
    extends WithRequestSession[F]
    with PartitionedReadable[Asset, F]
    with Create[Asset, AssetCreate, F]
    with RetrieveByIdsWithIgnoreUnknownIds[Asset, F]
    with RetrieveByExternalIdsWithIgnoreUnknownIds[Asset, F]
    with DeleteByCogniteIds[F]
    with PartitionedFilter[Asset, AssetsFilter, F]
    with Search[Asset, AssetsQuery, F]
    with UpdateById[Asset, AssetUpdate, F]
    with UpdateByExternalId[Asset, AssetUpdate, F] {
  import Assets._
  override val baseUrl = uri"${requestSession.baseUrl}/assets"

  override private[sdk] def readWithCursor(
      cursor: Option[String],
      limit: Option[Int],
      partition: Option[Partition]
  ): F[ItemsWithCursor[Asset]] =
    Readable.readWithCursor(
      requestSession,
      baseUrl,
      cursor,
      limit,
      partition,
      Constants.defaultBatchSize
    )

  override def retrieveByIds(
      ids: Seq[Long],
      ignoreUnknownIds: Boolean
  ): F[Seq[Asset]] =
    RetrieveByIdsWithIgnoreUnknownIds.retrieveByIds(
      requestSession,
      baseUrl,
      ids,
      ignoreUnknownIds
    )

  override def retrieveByExternalIds(
      externalIds: Seq[String],
      ignoreUnknownIds: Boolean
  ): F[Seq[Asset]] =
    RetrieveByExternalIdsWithIgnoreUnknownIds.retrieveByExternalIds(
      requestSession,
      baseUrl,
      externalIds,
      ignoreUnknownIds
    )

  override def createItems(items: Items[AssetCreate]): F[Seq[Asset]] =
    Create.createItems[F, Asset, AssetCreate](requestSession, baseUrl, items)

  override def updateById(items: Map[Long, AssetUpdate]): F[Seq[Asset]] =
    UpdateById.updateById[F, Asset, AssetUpdate](requestSession, baseUrl, items)

  override def updateByExternalId(items: Map[String, AssetUpdate]): F[Seq[Asset]] =
    UpdateByExternalId.updateByExternalId[F, Asset, AssetUpdate](
      requestSession,
      baseUrl,
      items
    )

  @deprecated("Please use deleteRecursive instead", "1.5.22")
  def deleteByIds(
      ids: Seq[Long],
      recursive: Boolean,
      ignoreUnknownIds: Boolean
  ): F[Unit] =
    deleteRecursive(ids.map(CogniteInternalId.apply), recursive, ignoreUnknownIds)

  @deprecated("Please use deleteRecursive instead", "1.5.22")
  def deleteByExternalIds(
      externalIds: Seq[String],
      recursive: Boolean,
      ignoreUnknownIds: Boolean
  ): F[Unit] =
    deleteRecursive(externalIds.map(CogniteExternalId.apply), recursive, ignoreUnknownIds)

  def deleteRecursive(ids: Seq[CogniteId], recursive: Boolean, ignoreUnknownIds: Boolean): F[Unit] =
    requestSession.post[Unit, Unit, ItemsWithRecursiveAndIgnoreUnknownIds](
      ItemsWithRecursiveAndIgnoreUnknownIds(
        ids,
        recursive,
        ignoreUnknownIds
      ),
      uri"$baseUrl/delete",
      _ => ()
    )

  override def delete(ids: Seq[CogniteId], ignoreUnknownIds: Boolean = false): F[Unit] =
    DeleteByCogniteIds.deleteWithIgnoreUnknownIds(
      requestSession,
      baseUrl,
      ids,
      ignoreUnknownIds
    )

  def filter(
      filter: AssetsFilter,
      limit: Option[Int],
      aggregatedProperties: Option[Seq[String]]
  ): fs2.Stream[F, Asset] =
    filterWithNextCursor(filter, None, limit, aggregatedProperties)

  def filterPartitions(
      filter: AssetsFilter,
      numPartitions: Int,
      limitPerPartition: Option[Int],
      aggregatedProperties: Option[Seq[String]]
  ): Seq[fs2.Stream[F, Asset]] =
    1.to(numPartitions).map { i =>
      Readable
        .pullFromCursor(
          None,
          limitPerPartition,
          Some(Partition(i, numPartitions)),
          filterWithCursor(filter, _, _, _, aggregatedProperties)
        )
        .stream
    }

  private[sdk] def filterWithCursor(
      filter: AssetsFilter,
      cursor: Option[String],
      limit: Option[Int],
      partition: Option[Partition],
      aggregatedProperties: Option[Seq[String]] = None
  ): F[ItemsWithCursor[Asset]] =
    Filter.filterWithCursor(
      requestSession,
      uri"$baseUrl/list",
      filter,
      cursor,
      limit,
      partition,
      Constants.defaultBatchSize,
      aggregatedProperties
    )

  override def search(searchQuery: AssetsQuery): F[Seq[Asset]] =
    Search.search(requestSession, baseUrl, searchQuery)
}

object Assets {
  implicit val assetDecoder: Decoder[Asset] = deriveDecoder[Asset]
  implicit val assetsItemsWithCursorDecoder: Decoder[ItemsWithCursor[Asset]] =
    deriveDecoder[ItemsWithCursor[Asset]]
  implicit val assetsItemsDecoder: Decoder[Items[Asset]] =
    deriveDecoder[Items[Asset]]
  implicit val createAssetEncoder: Encoder[AssetCreate] = deriveEncoder[AssetCreate]
  implicit val createAssetsItemsEncoder: Encoder[Items[AssetCreate]] =
    deriveEncoder[Items[AssetCreate]]
  implicit val assetUpdateEncoder: Encoder[AssetUpdate] = deriveEncoder[AssetUpdate]
  implicit val assetsFilterEncoder: Encoder[AssetsFilter] =
    deriveEncoder[AssetsFilter]
  implicit val assetsSearchEncoder: Encoder[AssetsSearch] =
    deriveEncoder[AssetsSearch]
  implicit val assetsQueryEncoder: Encoder[AssetsQuery] =
    deriveEncoder[AssetsQuery]
  implicit val assetsFilterRequestEncoder: Encoder[FilterRequest[AssetsFilter]] =
    deriveEncoder[FilterRequest[AssetsFilter]]

  implicit val errorOrUnitDecoder: Decoder[Either[CdpApiError, Unit]] =
    EitherDecoder.eitherDecoder[CdpApiError, Unit]
  implicit val deleteRequestWithRecursiveAndIgnoreUnknownIdsEncoder
      : Encoder[ItemsWithRecursiveAndIgnoreUnknownIds] =
    deriveEncoder[ItemsWithRecursiveAndIgnoreUnknownIds]
  implicit val cogniteExternalIdDecoder: Decoder[CogniteExternalId] =
    deriveDecoder[CogniteExternalId]
}
