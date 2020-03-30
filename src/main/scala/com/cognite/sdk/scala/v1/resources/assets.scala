package com.cognite.sdk.scala.v1.resources

import com.cognite.sdk.scala.common._
import com.cognite.sdk.scala.v1._
import com.softwaremill.sttp._
import com.softwaremill.sttp.circe._
import io.circe.{Decoder, Encoder}
import io.circe.derivation.{deriveDecoder, deriveEncoder}

class Assets[F[_]](val requestSession: RequestSession[F])
    extends WithRequestSession[F]
    with PartitionedReadable[Asset, F]
    with Create[Asset, AssetCreate, F]
    with RetrieveByIdsWithIgnoreUnknownIds[Asset, F]
    with RetrieveByExternalIdsWithIgnoreUnknownIds[Asset, F]
    with DeleteByIdsWithIgnoreUnknownIds[F, Long]
    with DeleteByExternalIdsWithIgnoreUnknownIds[F]
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
        ids.map(CogniteInternalId),
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
        externalIds.map(CogniteExternalId),
        recursive,
        ignoreUnknownIds
      ),
      uri"$baseUrl/delete",
      _ => ()
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
}
