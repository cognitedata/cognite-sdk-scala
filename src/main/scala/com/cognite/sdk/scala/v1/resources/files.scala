package com.cognite.sdk.scala.v1.resources

import com.cognite.sdk.scala.common._
import com.cognite.sdk.scala.v1.{CreateFile, File}
import com.softwaremill.sttp._
import com.softwaremill.sttp.circe._
import io.circe.{Decoder, Encoder}
import io.circe.generic.auto._

class Files[F[_]](project: String)(implicit auth: Auth)
    extends ReadWritableResourceV1[File, CreateFile, F]
    with ResourceV1[F] {
  override val baseUri = uri"https://api.cognitedata.com/api/v1/projects/$project/files"

  implicit val errorOrFileDecoder: Decoder[Either[CdpApiError[CogniteId], File]] =
    EitherDecoder.eitherDecoder[CdpApiError[CogniteId], File]
  override def createItems(
      items: Items[CreateFile]
  )(
      implicit sttpBackend: SttpBackend[F, _],
      auth: Auth,
      extractor: Extractor[Id],
      errorDecoder: Decoder[CdpApiError[CogniteId]],
      itemsEncoder: Encoder[Items[CreateFile]],
      itemsWithCursorDecoder: Decoder[Id[ItemsWithCursor[File]]]
  ): F[Response[Seq[File]]] =
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
