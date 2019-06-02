package com.cognite.sdk.scala.v0_6

import com.cognite.sdk.scala.common._
import com.softwaremill.sttp._
import io.circe.{Decoder, Encoder}

final case class Asset(
    id: Long = 0,
    path: Option[Seq[Long]] = None,
    depth: Option[Long] = None,
    name: String,
    parentId: Option[Long] = None,
    description: Option[String] = None,
    metadata: Option[Map[String, String]] = None,
    source: Option[String] = None,
    sourceId: Option[String] = None,
    createdTime: Option[Long] = None,
    lastUpdatedTime: Option[Long] = None
) extends WithId

final case class CreateAsset(
    name: String,
    parentId: Option[Long] = None,
    description: Option[String] = None,
    source: Option[String] = None,
    sourceId: Option[String] = None,
    metadata: Option[Map[String, String]] = None
)

class Assets[F[_]](
    implicit val auth: Auth,
    val e: Extractor[Data],
    val sttpBackend: SttpBackend[F, _],
    val readDecoder: Decoder[Asset],
    val writeDecoder: Decoder[CreateAsset],
    val writeEncoder: Encoder[CreateAsset],
    val containerItemsWithCursorDecoder: Decoder[Data[ItemsWithCursor[Asset]]],
    val containerItemsDecoder: Decoder[Data[Items[Asset]]],
    val extractor: Extractor[Data]
) extends Resource[F]
    with ReadableResource[Asset, F, Data]
    with WritableResource[Asset, CreateAsset, F, Data] {
  override val baseUri = uri"https://api.cognitedata.com/api/0.6/projects/playground/assets"
}
