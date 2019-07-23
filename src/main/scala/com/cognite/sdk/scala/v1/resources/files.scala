package com.cognite.sdk.scala.v1.resources

import com.cognite.sdk.scala.common._
import com.cognite.sdk.scala.v1._
import com.softwaremill.sttp._
import com.softwaremill.sttp.circe._
import io.circe.{Decoder, Encoder}

class Files[F[_]](val requestSession: RequestSession)
    extends WithRequestSession
    with Readable[File, F]
    with RetrieveByIds[File, F]
    with Create[File, CreateFile, F]
    with DeleteByIdsV1[File, CreateFile, F]
    with DeleteByExternalIdsV1[F]
    with Filter[File, FilesFilter, F]
    with Search[File, FilesQuery, F]
    with Update[File, FileUpdate, F] {
  override val baseUri = uri"${requestSession.baseUri}/files"

  implicit val errorOrFileDecoder: Decoder[Either[CdpApiError, File]] =
    EitherDecoder.eitherDecoder[CdpApiError, File]
  override def createItems(
      items: Items[CreateFile]
  )(
      implicit sttpBackend: SttpBackend[F, _],
      errorDecoder: Decoder[CdpApiError],
      itemsEncoder: Encoder[Items[CreateFile]],
      itemsWithCursorDecoder: Decoder[ItemsWithCursor[File]]
  ): F[Response[Seq[File]]] =
    items.items match {
      case item :: Nil =>
        requestSession
          .request
          .post(baseUri)
          .body(item)
          .response(asJson[Either[CdpApiError, File]])
          .mapResponse {
            case Left(value) => throw value.error
            case Right(Left(cdpApiError)) => throw cdpApiError.asException(uri"$baseUri/byids")
            case Right(Right(value)) => Seq(value)
          }
          .send()
      case _ => throw new RuntimeException("Files only support creating one file per call")
    }
}
