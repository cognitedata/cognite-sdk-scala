package com.cognite.sdk.scala.v1_0

import com.cognite.sdk.scala.common.{Auth, Items, ItemsWithCursor, WithId}
import com.softwaremill.sttp._
import com.softwaremill.sttp.circe._
import io.circe.{Decoder, Encoder}

final case class File(
    id: Long = 0,
    name: String,
    source: Option[String] = None,
    externalId: Option[String] = None,
    mimeType: Option[String] = None,
    metadata: Option[Map[String, String]] = None,
    assetIds: Option[Seq[Long]] = None,
    uploaded: Boolean = false,
    uploadedTime: Option[Long] = None,
    createdTime: Long = 0,
    lastUpdatedTime: Long = 0,
    uploadUrl: Option[String] = None
) extends WithId

final case class CreateFile(
    name: String,
    source: Option[String] = None,
    externalId: Option[String] = None,
    mimeType: Option[String] = None,
    metadata: Option[Map[String, String]] = None,
    assetIds: Option[Seq[Long]] = None
)

class Files[F[_]](
    implicit val auth: Auth,
    val sttpBackend: SttpBackend[F, _],
    val readDecoder: Decoder[File],
    val containerItemsWithCursorDecoder: Decoder[Id[ItemsWithCursor[File]]],
    val containerItemsDecoder: Decoder[Id[Items[File]]],
    val writeDecoder: Decoder[CreateFile],
    val writeEncoder: Encoder[CreateFile]
) extends ReadWritableResourceV1[File, CreateFile, F] with ResourceV1[F] {
  override val baseUri = uri"https://api.cognitedata.com/api/v1/projects/playground/files"

  override def createItems(items: Items[CreateFile]): F[Response[Seq[File]]] =
    items.items match {
      case item :: Nil =>
        request
          .post(baseUri)
          .body(item)
          .response(asJson[File])
          .mapResponse {
            case Left(value) => throw value.error
            case Right(value) => Seq(value)
          }
          .send()
      case _ => throw new RuntimeException("Files only support creating one file per call")
    }
}
