package com.cognite.sdk.scala.v1_0

import com.cognite.sdk.scala.common.{Auth, ItemsWithCursor}
import com.softwaremill.sttp._
import io.circe.Decoder

final case class Files(
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
    val readDecoder: Decoder[Files],
    val containerDecoder: Decoder[Id[ItemsWithCursor[Files]]],
) extends ResourceV1[F]
    with ReadableResourceV1[Files, F] {
  override val baseUri = uri"https://api.cognitedata.com/api/0.6/projects/playground/files"
}
