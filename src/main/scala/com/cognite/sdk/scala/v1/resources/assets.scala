package com.cognite.sdk.scala.v1.resources

import com.cognite.sdk.scala.common._
import com.cognite.sdk.scala.v1.{Asset, AssetUpdate, AssetsFilter, AssetsQuery, CreateAsset}
import com.softwaremill.sttp._

class Assets[F[_]](val requestSession: RequestSession)
    extends WithRequestSession
    with Readable[Asset, F]
    with Create[Asset, CreateAsset, F]
    with RetrieveByIds[Asset, F]
    with DeleteByIdsV1[Asset, CreateAsset, F]
    with DeleteByExternalIdsV1[F]
    with Filter[Asset, AssetsFilter, F]
    with Search[Asset, AssetsQuery, F]
    with Update[Asset, AssetUpdate, F] {
  override val baseUri = uri"${requestSession.baseUri}/assets"
}
