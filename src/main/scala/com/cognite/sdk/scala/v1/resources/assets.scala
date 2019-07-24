package com.cognite.sdk.scala.v1.resources

import com.cognite.sdk.scala.common._
import com.cognite.sdk.scala.v1._
import com.softwaremill.sttp._
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto._

class Assets[F[_]](val requestSession: RequestSession[F])
    extends WithRequestSession[F]
    with Readable[Asset, F]
    with Create[Asset, CreateAsset, F]
    with RetrieveByIds[Asset, F]
    with DeleteByIds[F, Long]
    with DeleteByExternalIds[F]
    with Filter[Asset, AssetsFilter, F]
    with Search[Asset, AssetsQuery, F]
    with Update[Asset, AssetUpdate, F] {
  import Assets._
  override val baseUri = uri"${requestSession.baseUri}/assets"

  override def readWithCursor(
      cursor: Option[String],
      limit: Option[Long]
  ): F[Response[ItemsWithCursor[Asset]]] =
    Readable.readWithCursor(requestSession, baseUri, cursor, limit)

  override def retrieveByIds(ids: Seq[Long]): F[Response[Seq[Asset]]] =
    RetrieveByIds.retrieveByIds(requestSession, baseUri, ids)

  override def createItems(items: Items[CreateAsset]): F[Response[Seq[Asset]]] =
    Create.createItems[F, Asset, CreateAsset](requestSession, baseUri, items)

  override def updateItems(items: Seq[AssetUpdate]): F[Response[Seq[Asset]]] =
    Update.updateItems[F, Asset, AssetUpdate](requestSession, baseUri, items)

  override def deleteByIds(ids: Seq[Long]): F[Response[Unit]] =
    DeleteByIds.deleteByIds(requestSession, baseUri, ids)

  override def deleteByExternalIds(externalIds: Seq[String]): F[Response[Unit]] =
    DeleteByExternalIds.deleteByExternalIds(requestSession, baseUri, externalIds)

  override def filterWithCursor(
      filter: AssetsFilter,
      cursor: Option[String],
      limit: Option[Long]
  ): F[Response[ItemsWithCursor[Asset]]] =
    Filter.filterWithCursor(requestSession, baseUri, filter, cursor, limit)

  override def search(searchQuery: AssetsQuery): F[Response[Seq[Asset]]] =
    Search.search(requestSession, baseUri, searchQuery)
}

object Assets {
  implicit val assetDecoder: Decoder[Asset] = deriveDecoder[Asset]
  implicit val assetsItemsWithCursorDecoder: Decoder[ItemsWithCursor[Asset]] =
    deriveDecoder[ItemsWithCursor[Asset]]
  implicit val assetsItemsDecoder: Decoder[Items[Asset]] =
    deriveDecoder[Items[Asset]]
  implicit val createAssetEncoder: Encoder[CreateAsset] = deriveEncoder[CreateAsset]
  implicit val createAssetsItemsEncoder: Encoder[Items[CreateAsset]] =
    deriveEncoder[Items[CreateAsset]]
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
