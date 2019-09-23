package com.cognite.sdk.scala.v1.resources

import java.time.Instant

import com.cognite.sdk.scala.common._
import com.cognite.sdk.scala.v1._
import com.softwaremill.sttp._
import io.circe.{Decoder, Encoder}
import io.circe.derivation.{deriveDecoder, deriveEncoder}

class Assets[F[_]](val requestSession: RequestSession[F])
    extends WithRequestSession[F]
    with PartitionedReadable[Asset, F]
    with Create[Asset, AssetCreate, F]
    with RetrieveByIds[Asset, F]
    with RetrieveByExternalIds[Asset, F]
    with DeleteByIds[F, Long]
    with DeleteByExternalIds[F]
    with PartitionedFilter[Asset, AssetsFilter, F]
    with Search[Asset, AssetsQuery, F]
    with Update[Asset, AssetUpdate, F] {
  import Assets._
  override val baseUri = uri"${requestSession.baseUri}/assets"

  override private[sdk] def readWithCursor(
      cursor: Option[String],
      limit: Option[Long],
      partition: Option[Partition]
  ): F[ItemsWithCursor[Asset]] =
    Readable.readWithCursor(requestSession, baseUri, cursor, limit, partition)

  override def retrieveByIds(ids: Seq[Long]): F[Seq[Asset]] =
    RetrieveByIds.retrieveByIds(requestSession, baseUri, ids)

  override def retrieveByExternalIds(externalIds: Seq[String]): F[Seq[Asset]] =
    RetrieveByExternalIds.retrieveByExternalIds(requestSession, baseUri, externalIds)

  override def createItems(items: Items[AssetCreate]): F[Seq[Asset]] =
    Create.createItems[F, Asset, AssetCreate](requestSession, baseUri, items)

  override def update(items: Seq[AssetUpdate]): F[Seq[Asset]] =
    Update.update[F, Asset, AssetUpdate](requestSession, baseUri, items)

  override def deleteByIds(ids: Seq[Long]): F[Unit] =
    DeleteByIds.deleteByIds(requestSession, baseUri, ids)

  override def deleteByExternalIds(externalIds: Seq[String]): F[Unit] =
    DeleteByExternalIds.deleteByExternalIds(requestSession, baseUri, externalIds)

  private[sdk] def filterWithCursor(
      filter: AssetsFilter,
      cursor: Option[String],
      limit: Option[Long],
      partition: Option[Partition]
  ): F[ItemsWithCursor[Asset]] =
    Filter.filterWithCursor(requestSession, baseUri, filter, cursor, limit, partition)

  override def search(searchQuery: AssetsQuery): F[Seq[Asset]] =
    Search.search(requestSession, baseUri, searchQuery)
}

object Assets {
  implicit val instantEncoder: Encoder[Instant] = Encoder.encodeLong.contramap(_.toEpochMilli)
  implicit val instantDecoder: Decoder[Instant] = Decoder.decodeLong.map(Instant.ofEpochMilli)

  implicit val assetDecoder: Decoder[Asset] = deriveDecoder[Asset]
  implicit val assetsItemsWithCursorDecoder: Decoder[ItemsWithCursor[Asset]] =
    deriveDecoder[ItemsWithCursor[Asset]]
  implicit val assetsItemsDecoder: Decoder[Items[Asset]] =
    deriveDecoder[Items[Asset]]
  implicit val createAssetEncoder: Encoder[AssetCreate] = deriveEncoder[AssetCreate]
  implicit val createAssetsItemsEncoder: Encoder[Items[AssetCreate]] =
    deriveEncoder[Items[AssetCreate]]
  implicit val assetUpdateEncoder: Encoder[AssetUpdate] =
    deriveEncoder[AssetUpdate]
  implicit val updateAssetsItemsEncoder: Encoder[Items[AssetUpdate]] =
    deriveEncoder[Items[AssetUpdate]]
  implicit val assetsFilterEncoder: Encoder[AssetsFilter] =
    deriveEncoder[AssetsFilter]
  implicit val assetsSearchEncoder: Encoder[AssetsSearch] =
    deriveEncoder[AssetsSearch]
  implicit val assetsQueryEncoder: Encoder[AssetsQuery] =
    deriveEncoder[AssetsQuery]
  implicit val assetsFilterRequestEncoder: Encoder[FilterRequest[AssetsFilter]] =
    deriveEncoder[FilterRequest[AssetsFilter]]
}
