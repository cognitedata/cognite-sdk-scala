package com.cognite.sdk.scala.common

import com.softwaremill.sttp._
import com.softwaremill.sttp.circe._
import io.circe.generic.auto._
import io.circe.{Decoder, Encoder}

abstract class ReadableResource[R: Decoder, F[_], C[_], InternalId, PrimitiveId](
    implicit auth: Auth,
    containerItemsWithCursorDecoder: Decoder[C[ItemsWithCursor[R]]],
    sttpBackend: SttpBackend[F, _]
) extends Resource[F, InternalId, PrimitiveId](auth) {
  implicit val extractor: Extractor[C]
  implicit val idEncoder: Encoder[InternalId]

  private def readWithCursor(
      cursor: Option[String],
      limit: Option[Long]
  ): F[Response[ItemsWithCursor[R]]] =
    request
      .get(
        cursor
          .fold(baseUri)(baseUri.param("cursor", _))
          .param("limit", limit.getOrElse(defaultLimit).toString)
      )
      .response(asJson[C[ItemsWithCursor[R]]])
      .mapResponse {
        case Left(value) => throw value.error
        case Right(value) => extractor.extract(value)
      }
      .send()

  def readFromCursor(cursor: String): F[Response[ItemsWithCursor[R]]] =
    readWithCursor(Some(cursor), None)
  def readFromCursorWithLimit(cursor: String, limit: Long): F[Response[ItemsWithCursor[R]]] =
    readWithCursor(Some(cursor), Some(limit))
  def read(): F[Response[ItemsWithCursor[R]]] = readWithCursor(None, None)
  def readWithLimit(limit: Long): F[Response[ItemsWithCursor[R]]] =
    readWithCursor(None, Some(limit))

  private def readWithNextCursor(cursor: Option[String], limit: Option[Long]): Iterator[F[Response[Seq[R]]]] =
    new NextCursorIterator[R, F](cursor, limit) {
      def get(
          cursor: Option[String],
          remainingItems: Option[Long]
      ): F[Response[ItemsWithCursor[R]]] =
        readWithCursor(cursor, remainingItems)
    }

  def readAllFromCursor(cursor: String): Iterator[F[Response[Seq[R]]]] =
    readWithNextCursor(Some(cursor), None)
  def readAllWithLimit(limit: Long): Iterator[F[Response[Seq[R]]]] = readWithNextCursor(None, Some(limit))
  def readAllFromCursorWithLimit(cursor: String, limit: Long): Iterator[F[Response[Seq[R]]]] =
    readWithNextCursor(Some(cursor), Some(limit))
  def readAll(): Iterator[F[Response[Seq[R]]]] = readWithNextCursor(None, None)
}

abstract class ReadableResourceWithRetrieve[R: Decoder, F[_], C[_], InternalId, PrimitiveId](
  implicit auth: Auth,
  containerItemsDecoder: Decoder[C[Items[R]]],
  containerItemsWithCursorDecoder: Decoder[C[ItemsWithCursor[R]]],
  sttpBackend: SttpBackend[F, _]
) extends ReadableResource[R, F, C, InternalId, PrimitiveId] {
  implicit val errorOrItemsDecoder: Decoder[Either[CdpApiError[CogniteId], C[Items[R]]]] =
    EitherDecoder.eitherDecoder[CdpApiError[CogniteId], C[Items[R]]]
  def retrieveByIds(ids: Seq[PrimitiveId]): F[Response[Seq[R]]] =
    request
      .get(uri"$baseUri/byids")
      .body(Items(ids.map(toInternalId)))
      .response(asJson[Either[CdpApiError[CogniteId], C[Items[R]]]])
      .mapResponse {
        case Left(value) => throw value.error
        case Right(Left(cdpApiError)) => throw cdpApiError.asException(uri"$baseUri/byids")
        case Right(Right(value)) => extractor.extract(value).items
      }
      .send()
}
