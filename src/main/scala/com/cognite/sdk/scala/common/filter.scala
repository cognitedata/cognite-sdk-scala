package com.cognite.sdk.scala.common

import com.softwaremill.sttp._
import com.softwaremill.sttp.circe._
import io.circe.generic.auto._
import io.circe.{Decoder, Encoder}

final case class FilterRequest[T](filter: T, limit: Option[Long], cursor: Option[String])

trait Filter[R, Fi, F[_], C[_]] extends RequestSession with BaseUri {
  lazy val filterUri = uri"$baseUri/list"

  // scalastyle:off
  private def filterWithCursor(filter: Fi, cursor: Option[String], limit: Option[Long])(
      implicit sttpBackend: SttpBackend[F, _],
      auth: Auth,
      extractor: Extractor[C],
      errorDecoder: Decoder[CdpApiError],
      filterEncoder: Encoder[Fi],
      itemsDecoder: Decoder[C[ItemsWithCursor[R]]]
  ): F[Response[ItemsWithCursor[R]]] = {
    implicit val errorOrItemsDecoder: Decoder[Either[CdpApiError, C[ItemsWithCursor[R]]]] =
      EitherDecoder.eitherDecoder[CdpApiError, C[ItemsWithCursor[R]]]
    request
      .post(filterUri)
      .body(FilterRequest(filter, limit, cursor))
      .response(asJson[Either[CdpApiError, C[ItemsWithCursor[R]]]])
      .mapResponse {
        case Left(value) => throw value.error
        case Right(Left(cdpApiError)) => throw cdpApiError.asException(filterUri)
        case Right(Right(value)) => extractor.extract(value)
      }
      .send()
  }

  private def filterWithNextCursor(filter: Fi, cursor: Option[String], limit: Option[Long])(
      implicit sttpBackend: SttpBackend[F, _],
      auth: Auth,
      extractor: Extractor[C],
      errorDecoder: Decoder[CdpApiError],
      filterEncoder: Encoder[Fi],
      itemsDecoder: Decoder[C[ItemsWithCursor[R]]]
  ): NextCursorIterator[R, F] = {
    // filter is also a member of Iterator, so rename this here
    val theFilter = filter
    new NextCursorIterator[R, F](cursor, limit) {
      def get(
          cursor: Option[String],
          remainingItems: Option[Long]
      ): F[Response[ItemsWithCursor[R]]] =
        filterWithCursor(theFilter, cursor, remainingItems)
    }
  }

  def filter(filter: Fi)(
      implicit sttpBackend: SttpBackend[F, _],
      auth: Auth,
      extractor: Extractor[C],
      errorDecoder: Decoder[CdpApiError],
      filterEncoder: Encoder[Fi],
      itemsDecoder: Decoder[C[ItemsWithCursor[R]]]
  ): Iterator[F[Response[Seq[R]]]] =
    filterWithNextCursor(filter, None, None)

  def filterWithLimit(filter: Fi, limit: Long)(
      implicit sttpBackend: SttpBackend[F, _],
      auth: Auth,
      extractor: Extractor[C],
      errorDecoder: Decoder[CdpApiError],
      filterEncoder: Encoder[Fi],
      itemsDecoder: Decoder[C[ItemsWithCursor[R]]]
  ): Iterator[F[Response[Seq[R]]]] =
    filterWithNextCursor(filter, None, Some(limit))
}
