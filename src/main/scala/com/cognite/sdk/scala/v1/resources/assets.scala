// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1.resources

import com.cognite.sdk.scala.common._
import com.cognite.sdk.scala.v1._
import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker
import sttp.client3._
import sttp.client3.jsoniter_scala._

class Assets[F[_]](val requestSession: RequestSession[F])
    extends WithRequestSession[F]
    with Create[Asset, AssetCreate, F]
    with RetrieveByIdsWithIgnoreUnknownIds[Asset, F]
    with RetrieveByExternalIdsWithIgnoreUnknownIds[Asset, F]
    with DeleteByIdsWithIgnoreUnknownIds[F, Long]
    with DeleteByExternalIdsWithIgnoreUnknownIds[F]
    with Search[Asset, AssetsQuery, F]
    with UpdateById[Asset, AssetUpdate, F]
    with UpdateByExternalId[Asset, AssetUpdate, F] {
  import Assets._
  override val baseUrl = uri"${requestSession.baseUrl}/assets"

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

  override def deleteByIds(ids: Seq[Long]): F[Unit] = deleteByIds(ids, false)

  override def deleteByIds(ids: Seq[Long], ignoreUnknownIds: Boolean): F[Unit] =
    DeleteByIds.deleteByIdsWithIgnoreUnknownIds(requestSession, baseUrl, ids, ignoreUnknownIds)

  def deleteByIds(
      ids: Seq[Long],
      recursive: Boolean,
      ignoreUnknownIds: Boolean
  ): F[Unit] =
    requestSession.post[Unit, Unit, ItemsWithRecursiveAndIgnoreUnknownIds](
      ItemsWithRecursiveAndIgnoreUnknownIds(
        ids.map(CogniteInternalId.apply),
        recursive,
        ignoreUnknownIds
      ),
      uri"$baseUrl/delete",
      _ => ()
    )

  override def deleteByExternalIds(externalIds: Seq[String]): F[Unit] =
    deleteByExternalIds(externalIds, false)

  override def deleteByExternalIds(externalIds: Seq[String], ignoreUnknownIds: Boolean): F[Unit] =
    DeleteByExternalIds.deleteByExternalIdsWithIgnoreUnknownIds(
      requestSession,
      baseUrl,
      externalIds,
      ignoreUnknownIds
    )

  def deleteByExternalIds(
      externalIds: Seq[String],
      recursive: Boolean,
      ignoreUnknownIds: Boolean
  ): F[Unit] =
    requestSession.post[Unit, Unit, ItemsWithRecursiveAndIgnoreUnknownIds](
      ItemsWithRecursiveAndIgnoreUnknownIds(
        externalIds.map(CogniteExternalId.apply),
        recursive,
        ignoreUnknownIds
      ),
      uri"$baseUrl/delete",
      _ => ()
    )

  private[sdk] def filterWithCursor(
      filter: AssetsFilter,
      cursor: Option[String],
      limit: Option[Int],
      partition: Option[Partition],
      aggregatedProperties: Option[Seq[String]] = None
  ): F[ItemsWithCursor[Asset]] =
    Filter.filterWithCursor(
      requestSession,
      baseUrl,
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
  implicit val assetCodec: JsonValueCodec[Asset] = JsonCodecMaker.make[Asset]
  implicit val assetsItemsWithCursorCodec: JsonValueCodec[ItemsWithCursor[Asset]] =
    JsonCodecMaker.make[ItemsWithCursor[Asset]]
  implicit val assetsItemsCodec: JsonValueCodec[Items[Asset]] =
    JsonCodecMaker.make[Items[Asset]]
  implicit val createAssetCodec: JsonValueCodec[AssetCreate] = JsonCodecMaker.make[AssetCreate]
  implicit val createAssetsItemsCodec: JsonValueCodec[Items[AssetCreate]] =
    JsonCodecMaker.make[Items[AssetCreate]]
  implicit val assetUpdateCodec: JsonValueCodec[AssetUpdate] = JsonCodecMaker.make[AssetUpdate]
  implicit val assetsFilterCodec: JsonValueCodec[AssetsFilter] =
    JsonCodecMaker.make[AssetsFilter]
  implicit val assetsSearchCodec: JsonValueCodec[AssetsSearch] =
    JsonCodecMaker.make[AssetsSearch]
  implicit val assetsQueryCodec: JsonValueCodec[AssetsQuery] =
    JsonCodecMaker.make[AssetsQuery]
  implicit val assetsFilterRequestCodec: JsonValueCodec[FilterRequest[AssetsFilter]] =
    JsonCodecMaker.make[FilterRequest[AssetsFilter]]

  implicit val errorOrUnitCodec: JsonValueCodec[Either[CdpApiError, Unit]] =
    JsonCodecMaker.make[Either[CdpApiError, Unit]]
  implicit val deleteRequestWithRecursiveAndIgnoreUnknownIdsCodec
      : JsonValueCodec[ItemsWithRecursiveAndIgnoreUnknownIds] =
    JsonCodecMaker.make[ItemsWithRecursiveAndIgnoreUnknownIds]
}
