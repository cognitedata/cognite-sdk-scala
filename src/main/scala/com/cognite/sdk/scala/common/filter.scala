package com.cognite.sdk.scala.common

import com.cognite.sdk.scala.v1.RequestSession
import com.softwaremill.sttp._
import com.softwaremill.sttp.circe._
import io.circe.{Decoder, Encoder}

final case class FilterRequest[T](filter: T, limit: Option[Long], cursor: Option[String])

trait Filter[R, Fi, F[_]] extends WithRequestSession[F] with BaseUri {
  def filterWithCursor(
      filter: Fi,
      cursor: Option[String],
      limit: Option[Long]
  ): F[Response[ItemsWithCursor[R]]]

  private def filterWithNextCursor(
      filter: Fi,
      cursor: Option[String],
      limit: Option[Long]
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

  def filter(filter: Fi): Iterator[F[Response[Seq[R]]]] =
    filterWithNextCursor(filter, None, None)

  def filterWithLimit(filter: Fi, limit: Long): Iterator[F[Response[Seq[R]]]] =
    filterWithNextCursor(filter, None, Some(limit))
}

object Filter {
  def filterWithCursor[F[_], R, Fi: Encoder](
      requestSession: RequestSession[F],
      baseUri: Uri,
      filter: Fi,
      cursor: Option[String],
      limit: Option[Long]
  )(
      implicit readItemsWithCursorDecoder: Decoder[ItemsWithCursor[R]],
      filterRequestEncoder: Encoder[FilterRequest[Fi]]
  ): F[Response[ItemsWithCursor[R]]] = {
    implicit val errorOrItemsDecoder: Decoder[Either[CdpApiError, ItemsWithCursor[R]]] =
      EitherDecoder.eitherDecoder[CdpApiError, ItemsWithCursor[R]]
    requestSession
      .send { request =>
        request
          .post(uri"$baseUri/list")
          .body(FilterRequest(filter, limit, cursor))
          .response(asJson[Either[CdpApiError, ItemsWithCursor[R]]])
          .mapResponse {
            case Left(value) => throw value.error
            case Right(Left(cdpApiError)) => throw cdpApiError.asException(uri"$baseUri/list")
            case Right(Right(value)) => value
          }
      }
  }
}
