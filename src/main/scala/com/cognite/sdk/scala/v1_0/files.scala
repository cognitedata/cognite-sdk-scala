package com.cognite.sdk.scala.v1_0

import com.cognite.sdk.scala.common.{Auth, CdpApiError, CogniteId, EitherDecoder, Items, WithId}
import com.softwaremill.sttp._
import com.softwaremill.sttp.circe._
import io.circe.Decoder
import io.circe.generic.auto._

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

class Files[F[_]](implicit auth: Auth, sttpBackend: SttpBackend[F, _])
    extends ReadWritableResourceV1[File, CreateFile, F]
    with ResourceV1[F] {
  override val baseUri = uri"https://api.cognitedata.com/api/v1/projects/playground/files"

  implicit val errorOrFileDecoder: Decoder[Either[CdpApiError[CogniteId], File]] =
    EitherDecoder.eitherDecoder[CdpApiError[CogniteId], File]
  override def createItems(items: Items[CreateFile]): F[Response[Seq[File]]] =
    items.items match {
      case item :: Nil =>
        request
          .post(baseUri)
          .body(item)
          .response(asJson[Either[CdpApiError[CogniteId], File]])
          .mapResponse {
            case Left(value) => throw value.error
            case Right(Left(cdpApiError)) => throw cdpApiError.asException(uri"$baseUri/byids")
            case Right(Right(value)) => Seq(value)
          }
          .send()
      case _ => throw new RuntimeException("Files only support creating one file per call")
    }
}
