package com.cognite.sdk.scala.common

import com.softwaremill.sttp._
import com.softwaremill.sttp.circe._
import io.circe.{Decoder, Encoder}

trait Readable[R, F[_], C[_]] extends WithRequestSession with BaseUri {
  private def readWithCursor(cursor: Option[String], limit: Option[Long])(
      implicit sttpBackend: SttpBackend[F, _],
      auth: Auth,
      extractor: Extractor[C],
      errorDecoder: Decoder[CdpApiError],
      itemsDecoder: Decoder[C[ItemsWithCursor[R]]]
  ): F[Response[ItemsWithCursor[R]]] = {
    implicit val errorOrItemsDecoder: Decoder[Either[CdpApiError, C[ItemsWithCursor[R]]]] =
      EitherDecoder.eitherDecoder[CdpApiError, C[ItemsWithCursor[R]]]
    val uriWithCursor = cursor
      .fold(baseUri)(baseUri.param("cursor", _))
      .param("limit", limit.getOrElse(Resource.defaultLimit).toString)
    requestSession
      .request
      .get(uriWithCursor)
      .response(asJson[Either[CdpApiError, C[ItemsWithCursor[R]]]])
      .mapResponse {
        case Left(value) => throw value.error
        case Right(Left(cdpApiError)) => throw cdpApiError.asException(uriWithCursor)
        case Right(Right(value)) => extractor.extract(value)
      }
      .send()
  }

  def readFromCursor(cursor: String)(
      implicit sttpBackend: SttpBackend[F, _],
      auth: Auth,
      extractor: Extractor[C],
      errorDecoder: Decoder[CdpApiError],
      itemsDecoder: Decoder[C[ItemsWithCursor[R]]]
  ): F[Response[ItemsWithCursor[R]]] =
    readWithCursor(Some(cursor), None)

  def readFromCursorWithLimit(cursor: String, limit: Long)(
      implicit sttpBackend: SttpBackend[F, _],
      auth: Auth,
      extractor: Extractor[C],
      errorDecoder: Decoder[CdpApiError],
      itemsDecoder: Decoder[C[ItemsWithCursor[R]]]
  ): F[Response[ItemsWithCursor[R]]] =
    readWithCursor(Some(cursor), Some(limit))

  def read()(
      implicit sttpBackend: SttpBackend[F, _],
      auth: Auth,
      extractor: Extractor[C],
      errorDecoder: Decoder[CdpApiError],
      itemsDecoder: Decoder[C[ItemsWithCursor[R]]]
  ): F[Response[ItemsWithCursor[R]]] = readWithCursor(None, None)

  def readWithLimit(limit: Long)(
      implicit sttpBackend: SttpBackend[F, _],
      auth: Auth,
      extractor: Extractor[C],
      errorDecoder: Decoder[CdpApiError],
      itemsDecoder: Decoder[C[ItemsWithCursor[R]]]
  ): F[Response[ItemsWithCursor[R]]] =
    readWithCursor(None, Some(limit))

  private def readWithNextCursor(cursor: Option[String], limit: Option[Long])(
      implicit sttpBackend: SttpBackend[F, _],
      auth: Auth,
      extractor: Extractor[C],
      errorDecoder: Decoder[CdpApiError],
      itemsDecoder: Decoder[C[ItemsWithCursor[R]]]
  ): Iterator[F[Response[Seq[R]]]] =
    new NextCursorIterator[R, F](cursor, limit) {
      def get(
          cursor: Option[String],
          remainingItems: Option[Long]
      ): F[Response[ItemsWithCursor[R]]] =
        readWithCursor(cursor, remainingItems)
    }

  def readAllFromCursor(cursor: String)(
      implicit sttpBackend: SttpBackend[F, _],
      auth: Auth,
      extractor: Extractor[C],
      errorDecoder: Decoder[CdpApiError],
      itemsDecoder: Decoder[C[ItemsWithCursor[R]]]
  ): Iterator[F[Response[Seq[R]]]] =
    readWithNextCursor(Some(cursor), None)

  def readAllWithLimit(limit: Long)(
      implicit sttpBackend: SttpBackend[F, _],
      auth: Auth,
      extractor: Extractor[C],
      errorDecoder: Decoder[CdpApiError],
      itemsDecoder: Decoder[C[ItemsWithCursor[R]]]
  ): Iterator[F[Response[Seq[R]]]] =
    readWithNextCursor(None, Some(limit))

  def readAllFromCursorWithLimit(cursor: String, limit: Long)(
      implicit sttpBackend: SttpBackend[F, _],
      auth: Auth,
      extractor: Extractor[C],
      errorDecoder: Decoder[CdpApiError],
      itemsDecoder: Decoder[C[ItemsWithCursor[R]]]
  ): Iterator[F[Response[Seq[R]]]] =
    readWithNextCursor(Some(cursor), Some(limit))

  def readAll()(
      implicit sttpBackend: SttpBackend[F, _],
      auth: Auth,
      extractor: Extractor[C],
      errorDecoder: Decoder[CdpApiError],
      itemsDecoder: Decoder[C[ItemsWithCursor[R]]]
  ): Iterator[F[Response[Seq[R]]]] = readWithNextCursor(None, None)
}

trait RetrieveByIds[R, F[_], C[_]]
    extends WithRequestSession
    with BaseUri {

  def retrieveByIds(ids: Seq[Long])(
      implicit sttpBackend: SttpBackend[F, _],
      auth: Auth,
      extractor: Extractor[C],
      errorDecoder: Decoder[CdpApiError],
      itemsDecoder: Decoder[C[Items[R]]],
      d1: Encoder[Items[CogniteId]]
  ): F[Response[Seq[R]]] = {
    implicit val errorOrItemsDecoder: Decoder[Either[CdpApiError, C[Items[R]]]] =
      EitherDecoder.eitherDecoder[CdpApiError, C[Items[R]]]
    requestSession
      .request
      .get(uri"$baseUri/byids")
      .body(Items(ids.map(CogniteId)))
      .response(asJson[Either[CdpApiError, C[Items[R]]]])
      .mapResponse {
        case Left(value) => throw value.error
        case Right(Left(cdpApiError)) => throw cdpApiError.asException(uri"$baseUri/byids")
        case Right(Right(value)) => extractor.extract(value).items
      }
      .send()
  }
}
