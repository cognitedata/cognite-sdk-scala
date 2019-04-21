package com.cognite.sdk.scala.v0_6

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
    sourceId: Option[String] = None,
    createdTime: Option[Long] = None,
    lastUpdatedTime: Option[Long] = None)

final case class PostAsset(
    name: String,
    parentId: Option[Long] = None,
    description: Option[String] = None,
    source: Option[String] = None,
    sourceId: Option[String] = None,
    metadata: Option[Map[String, String]] = None
)

class Assets[F[_]](
    implicit val auth: Auth,
    val sttpBackend: SttpBackend[F, _],
    val readDecoder: Decoder[Asset],
    val writeDecoder: Decoder[PostAsset],
    val writeEncoder: Encoder[PostAsset])
    extends ReadableResource[Asset, F]
    with WritableResource[Asset, PostAsset, F] {
  override val listUri = uri"https://api.cognitedata.com/api/0.6/projects/playground/assets"
  override val createUri = uri"https://api.cognitedata.com/api/0.6/projects/playground/assets"
}
