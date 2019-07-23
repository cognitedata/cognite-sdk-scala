package com.cognite.sdk.scala.v1.resources

import com.cognite.sdk.scala.common._
import com.cognite.sdk.scala.v1.{Asset, AssetUpdate, AssetsFilter, AssetsQuery, CreateAsset}
import com.softwaremill.sttp._

class Assets[F[_]](val requestSession: RequestSession)
    extends WithRequestSession
    with Readable[Asset, F, Id]
    with Create[Asset, CreateAsset, F, Id]
    with RetrieveByIds[Asset, F, Id]
    with DeleteByIdsV1[Asset, CreateAsset, F, Id]
    with DeleteByExternalIdsV1[F]
    with Filter[Asset, AssetsFilter, F, Id]
    with Search[Asset, AssetsQuery, F, Id]
    with Update[Asset, AssetUpdate, F, Id] {
  override val baseUri = uri"${requestSession.baseUri}/assets"
}
