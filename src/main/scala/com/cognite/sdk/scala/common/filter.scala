package com.cognite.sdk.scala.common

import com.softwaremill.sttp._
import com.softwaremill.sttp.circe._
import io.circe.generic.semiauto._
import io.circe.{Decoder, Encoder}

final case class FilterRequest[T](filter: T, limit: Option[Long], cursor: Option[String])

trait Filter[R, Fi, F[_]] extends WithRequestSession[F] with BaseUri {
  lazy val filterUri = uri"$baseUri/list"

  // scalastyle:off
  private def filterWithCursor(filter: Fi, cursor: Option[String], limit: Option[Long])(
      implicit errorDecoder: Decoder[CdpApiError],
      filterEncoder: Encoder[Fi],
      itemsDecoder: Decoder[ItemsWithCursor[R]]
  ): F[Response[ItemsWithCursor[R]]] = {
    implicit val filterRequestEncoder: Encoder[FilterRequest[Fi]] = deriveEncoder[FilterRequest[Fi]]
    implicit val errorOrItemsDecoder: Decoder[Either[CdpApiError, ItemsWithCursor[R]]] =
      EitherDecoder.eitherDecoder[CdpApiError, ItemsWithCursor[R]]
    requestSession
      .send { request =>
        request
          .post(filterUri)
          .body(FilterRequest(filter, limit, cursor))
          .response(asJson[Either[CdpApiError, ItemsWithCursor[R]]])
          .mapResponse {
            case Left(value) => throw value.error
            case Right(Left(cdpApiError)) => throw cdpApiError.asException(filterUri)
            case Right(Right(value)) => value
          }
      }
  }

  private def filterWithNextCursor(filter: Fi, cursor: Option[String], limit: Option[Long])(
      implicit errorDecoder: Decoder[CdpApiError],
      filterEncoder: Encoder[Fi],
      itemsDecoder: Decoder[ItemsWithCursor[R]]
  ): NextCursorIterator[R, F] = {
    // filter is also a member of Iterator, so rename this here
    val theFilter = filter
    new NextCursorIterator[R, F](cursor, limit, requestSession.sttpBackend) {
      def get(
          cursor: Option[String],
          remainingItems: Option[Long]
      ): F[Response[ItemsWithCursor[R]]] =
        filterWithCursor(theFilter, cursor, remainingItems)
    }
  }

  def filter(filter: Fi)(
      implicit errorDecoder: Decoder[CdpApiError],
      filterEncoder: Encoder[Fi],
      itemsDecoder: Decoder[ItemsWithCursor[R]]
  ): Iterator[F[Response[Seq[R]]]] =
    filterWithNextCursor(filter, None, None)

  def filterWithLimit(filter: Fi, limit: Long)(
      implicit errorDecoder: Decoder[CdpApiError],
      filterEncoder: Encoder[Fi],
      itemsDecoder: Decoder[ItemsWithCursor[R]]
  ): Iterator[F[Response[Seq[R]]]] =
    filterWithNextCursor(filter, None, Some(limit))
}
