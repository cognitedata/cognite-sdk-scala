package com.cognite.sdk.scala.v1_0

import com.cognite.sdk.scala.common.{
  Auth,
  Extractor,
  ItemsWithCursor,
  ReadableResource,
  Resource,
  WritableResource
}
import com.softwaremill.sttp._
import io.circe.{Decoder, Encoder}

final case class Asset(
    id: Option[Long] = None,
    path: Option[Seq[Long]] = None,
    depth: Option[Long] = None,
    name: String,
    parentId: Option[Long] = None,
    description: Option[String] = None,
    metadata: Option[Map[String, String]] = None,
    source: Option[String] = None,
    externalId: Option[String] = None,
    createdTime: Option[Long] = None,
    lastUpdatedTime: Option[Long] = None
)

final case class PostAsset(
    name: String,
    parentId: Option[Long] = None,
    description: Option[String] = None,
    source: Option[String] = None,
    externalId: Option[String] = None,
    metadata: Option[Map[String, String]] = None
)

class Assets[F[_]](
    implicit val auth: Auth,
    val sttpBackend: SttpBackend[F, _],
    val readDecoder: Decoder[Asset],
    val writeDecoder: Decoder[PostAsset],
    val writeEncoder: Encoder[PostAsset],
    val containerDecoder: Decoder[Id[ItemsWithCursor[Asset]]],
    val extractor: Extractor[Id]
) extends Resource
    with ReadableResource[Asset, F, Id]
    with WritableResource[Asset, PostAsset, F, Id] {
  override val baseUri = uri"https://api.cognitedata.com/api/v1/projects/playground/assets"
}
