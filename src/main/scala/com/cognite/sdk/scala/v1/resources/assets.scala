package com.cognite.sdk.scala.v1.resources

import com.cognite.sdk.scala.common._
import com.cognite.sdk.scala.v1.{
  Asset,
  AssetUpdate,
  AssetsFilter,
  AssetsQuery,
  CreateAsset,
  RequestSession
}
import com.softwaremill.sttp._

class Assets[F[_]](val requestSession: RequestSession[F])
    extends WithRequestSession[F]
    with Readable[Asset, F]
    with Create[Asset, CreateAsset, F]
    with RetrieveByIds[Asset, F]
    with DeleteByIdsV1[Asset, CreateAsset, F]
    with DeleteByExternalIdsV1[F]
    with Filter[Asset, AssetsFilter, F]
    with Search[Asset, AssetsQuery, F]
    with Update[Asset, AssetUpdate, F] {
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
}
