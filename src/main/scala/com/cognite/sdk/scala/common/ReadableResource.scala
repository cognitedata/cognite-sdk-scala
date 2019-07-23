package com.cognite.sdk.scala.common

import com.cognite.sdk.scala.v1._
import com.softwaremill.sttp._
import com.softwaremill.sttp.circe._
import io.circe.Decoder
import io.circe.generic.auto._

trait Readable[R, F[_]] extends WithRequestSession[F] with BaseUri {
  def readWithCursor(cursor: Option[String], limit: Option[Long]): F[Response[ItemsWithCursor[R]]]

  def readFromCursor(cursor: String): F[Response[ItemsWithCursor[R]]] =
    readWithCursor(Some(cursor), None)

  def readFromCursorWithLimit(cursor: String, limit: Long): F[Response[ItemsWithCursor[R]]] =
    readWithCursor(Some(cursor), Some(limit))

  def read(): F[Response[ItemsWithCursor[R]]] = readWithCursor(None, None)

  def readWithLimit(limit: Long): F[Response[ItemsWithCursor[R]]] =
    readWithCursor(None, Some(limit))

  private def readWithNextCursor(
      cursor: Option[String],
      limit: Option[Long]
  ): Iterator[F[Response[Seq[R]]]] =
    new NextCursorIterator[R, F](cursor, limit, requestSession.sttpBackend) {
      def get(
          cursor: Option[String],
          remainingItems: Option[Long]
      ): F[Response[ItemsWithCursor[R]]] =
        readWithCursor(cursor, remainingItems)
    }

  def readAllFromCursor(cursor: String): Iterator[F[Response[Seq[R]]]] =
    readWithNextCursor(Some(cursor), None)

  def readAllWithLimit(limit: Long): Iterator[F[Response[Seq[R]]]] =
    readWithNextCursor(None, Some(limit))

  def readAllFromCursorWithLimit(cursor: String, limit: Long): Iterator[F[Response[Seq[R]]]] =
    readWithNextCursor(Some(cursor), Some(limit))

  def readAll(): Iterator[F[Response[Seq[R]]]] = readWithNextCursor(None, None)
}

object Readable {
  def readWithCursor[F[_], R](
      requestSession: RequestSession[F],
      baseUri: Uri,
      cursor: Option[String],
      limit: Option[Long]
  )(
      implicit itemsWithCursorDecoder: Decoder[ItemsWithCursor[R]]
  ): F[Response[ItemsWithCursor[R]]] = {
    implicit val errorOrItemsDecoder: Decoder[Either[CdpApiError, ItemsWithCursor[R]]] =
      EitherDecoder.eitherDecoder[CdpApiError, ItemsWithCursor[R]]
    val uriWithCursor = cursor
      .fold(baseUri)(baseUri.param("cursor", _))
      .param("limit", limit.getOrElse(Resource.defaultLimit).toString)
    requestSession
      .send { request =>
        request
          .get(uriWithCursor)
          .response(asJson[Either[CdpApiError, ItemsWithCursor[R]]])
          .mapResponse {
            case Left(value) => throw value.error
            case Right(Left(cdpApiError)) => throw cdpApiError.asException(uriWithCursor)
            case Right(Right(value)) => value
          }
      }
  }
}
trait RetrieveByIds[R, F[_]] extends WithRequestSession[F] with BaseUri {
  def retrieveByIds(ids: Seq[Long]): F[Response[Seq[R]]]
}

object RetrieveByIds {
  def retrieveByIds[F[_], R](requestSession: RequestSession[F], baseUri: Uri, ids: Seq[Long])(
      implicit itemsDecoder: Decoder[Items[R]]
  ): F[Response[Seq[R]]] = {
    implicit val errorOrItemsDecoder: Decoder[Either[CdpApiError, Items[R]]] =
      EitherDecoder.eitherDecoder[CdpApiError, Items[R]]
    requestSession
      .send { request =>
        request
          .get(uri"$baseUri/byids")
          .body(Items(ids.map(CogniteId)))
          .response(asJson[Either[CdpApiError, Items[R]]])
          .mapResponse {
            case Left(value) => throw value.error
            case Right(Left(cdpApiError)) => throw cdpApiError.asException(uri"$baseUri/byids")
            case Right(Right(value)) => value.items
          }
      }
  }
}
