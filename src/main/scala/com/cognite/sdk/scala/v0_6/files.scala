package com.cognite.sdk.scala.v0_6

import com.cognite.sdk.scala.common.{Auth, Items, ItemsWithCursor, ReadableResource, WithId}
import com.softwaremill.sttp._
import io.circe.Decoder

final case class File(
    id: Long = 0,
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
) extends WithId

class FilesResource[F[_]](
    implicit val auth: Auth,
    val sttpBackend: SttpBackend[F, _],
    val readDecoder: Decoder[File],
    val containerItemsWithCursorDecoder: Decoder[Data[ItemsWithCursor[File]]],
    val containerItemsDecoder: Decoder[Data[Items[File]]]
) extends ReadableResource[File, F, Data, Long] with ResourceV0_6[F] {
  override val baseUri = uri"https://api.cognitedata.com/api/0.6/projects/playground/files"
}
