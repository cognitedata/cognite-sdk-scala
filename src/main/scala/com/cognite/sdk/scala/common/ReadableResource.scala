package com.cognite.sdk.scala.common

import com.softwaremill.sttp._
import com.softwaremill.sttp.circe._
import io.circe.{Decoder, Encoder}

trait Readable[R, F[_], C[_], InternalId, PrimitiveId] extends RequestSession with BaseUri {
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
    request
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

abstract class ReadableResource[R: Decoder, F[_], C[_], InternalId, PrimitiveId](
    implicit auth: Auth
) extends Resource[F, InternalId, PrimitiveId](auth)
    with Readable[R, F, C, InternalId, PrimitiveId] {}

abstract class ReadableResourceWithRetrieve[R: Decoder, F[_], C[_], InternalId, PrimitiveId](
    implicit auth: Auth
) extends ReadableResource[R, F, C, InternalId, PrimitiveId]
    with RetrieveByIds[R, F, C, InternalId, PrimitiveId] {}

trait RetrieveByIds[R, F[_], C[_], InternalId, PrimitiveId]
    extends RequestSession
    with BaseUri
    with ToInternalId[InternalId, PrimitiveId] {

  def retrieveByIds(ids: Seq[PrimitiveId])(
      implicit sttpBackend: SttpBackend[F, _],
      auth: Auth,
      extractor: Extractor[C],
      errorDecoder: Decoder[CdpApiError],
      itemsDecoder: Decoder[C[Items[R]]],
      d1: Encoder[Items[InternalId]]
  ): F[Response[Seq[R]]] = {
    implicit val errorOrItemsDecoder: Decoder[Either[CdpApiError, C[Items[R]]]] =
      EitherDecoder.eitherDecoder[CdpApiError, C[Items[R]]]
    request
      .get(uri"$baseUri/byids")
      .body(Items(ids.map(toInternalId)))
      .response(asJson[Either[CdpApiError, C[Items[R]]]])
      .mapResponse {
        case Left(value) =>
          println(s"decoding failure on ${value.original}")
          println(s"decoding failure message: ${value.message}")
          throw value.error
        case Right(Left(cdpApiError)) => throw cdpApiError.asException(uri"$baseUri/byids")
        case Right(Right(value)) => extractor.extract(value).items
      }
      .send()
  }
}
