package com.cognite.sdk.scala.v1.resources

import com.cognite.sdk.scala.common.{Auth, Filter, Search, Update}
import com.cognite.sdk.scala.v1.{Asset, AssetUpdate, AssetsFilter, AssetsQuery, CreateAsset}
import com.softwaremill.sttp._
import io.circe.generic.auto._

class Assets[F[_]](project: String)(implicit auth: Auth)
    extends ReadWritableResourceV1[Asset, CreateAsset, F]
    with ResourceV1[F]
    with Filter[Asset, AssetsFilter, F, Id]
    with Search[Asset, AssetsQuery, F, Id]
    with Update[Asset, AssetUpdate, F, Id] {
  override val baseUri = uri"https://api.cognitedata.com/api/v1/projects/$project/assets"
}
