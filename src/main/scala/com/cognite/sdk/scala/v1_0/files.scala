package com.cognite.sdk.scala.v1_0

import com.cognite.sdk.scala.common.{Auth, Items, ItemsWithCursor}
import com.softwaremill.sttp._
import io.circe.Decoder

final case class File(
    id: Option[Long] = None,
    fileName: String,
    directory: Option[String] = None,
    source: Option[String] = None,
    sourceId: Option[String] = None,
    fileType: Option[String] = None,
    metadata: Option[Map[String, String]] = None,
    assetIds: Option[Seq[Long]] = None,
    uploaded: Option[Boolean] = None,
    uploadedAt: Option[Long] = None,
    createdTime: Option[Long] = None,
    lastUpdatedTime: Option[Long] = None
)

class FilesResource[F[_]](
    implicit val auth: Auth,
    val sttpBackend: SttpBackend[F, _],
    val readDecoder: Decoder[File],
    val containerItemsWithCursorDecoder: Decoder[Id[ItemsWithCursor[File]]],
    val containerItemsDecoder: Decoder[Id[Items[File]]]
) extends ResourceV1[F]
    with ReadableResourceV1[File, F] {
  override val baseUri = uri"https://api.cognitedata.com/api/0.6/projects/playground/files"
}
