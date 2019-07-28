package com.cognite.sdk.scala.common

import com.cognite.sdk.scala.v1.RequestSession
import com.softwaremill.sttp._
import com.softwaremill.sttp.circe._
import fs2._
import io.circe.{Decoder, Encoder}

final case class FilterRequest[T](filter: T, limit: Option[Long], cursor: Option[String])

trait Filter[R, Fi, F[_]] extends WithRequestSession[F] with BaseUri {
  def filterWithCursor(
      filter: Fi,
      cursor: Option[String],
      limit: Option[Long]
  ): F[ItemsWithCursor[R]]

  private def filterWithNextCursor(
      filter: Fi,
      cursor: Option[String],
      limit: Option[Long]
  ): Stream[F, R] =
    Readable.pullFromCursorWithLimit(cursor, limit, filterWithCursor(filter, _, _)).stream

  def filter(filter: Fi): Stream[F, R] =
    filterWithNextCursor(filter, None, None)

  def filterWithLimit(filter: Fi, limit: Long): Stream[F, R] =
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
  ): F[ItemsWithCursor[R]] = {
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
