package com.cognite.sdk.scala.common

import com.cognite.sdk.scala.v1._
import com.softwaremill.sttp._
import com.softwaremill.sttp.circe._
import fs2._
import io.circe.{Decoder, Encoder}
import io.circe.derivation.deriveEncoder

trait Readable[R, F[_]] extends WithRequestSession[F] with BaseUri {
  def readWithCursor(cursor: Option[String], limit: Option[Long]): F[ItemsWithCursor[R]]

  def readFromCursor(cursor: String): F[ItemsWithCursor[R]] =
    readWithCursor(Some(cursor), None)

  def readFromCursorWithLimit(cursor: String, limit: Long): F[ItemsWithCursor[R]] =
    readWithCursor(Some(cursor), Some(limit))

  def read(): F[ItemsWithCursor[R]] = readWithCursor(None, None)

  def readWithLimit(limit: Long): F[ItemsWithCursor[R]] =
    readWithCursor(None, Some(limit))

  private def listWithNextCursor(
      cursor: Option[String],
      limit: Option[Long]
  ): Stream[F, R] =
    Readable.pullFromCursorWithLimit(cursor, limit, readWithCursor).stream

  def listFromCursor(cursor: String): Stream[F, R] =
    listWithNextCursor(Some(cursor), None)

  def listWithLimit(limit: Long): Stream[F, R] =
    listWithNextCursor(None, Some(limit))

  def listFromCursorWithLimit(cursor: String, limit: Long): Stream[F, R] =
    listWithNextCursor(Some(cursor), Some(limit))

  def list(): Stream[F, R] = listWithNextCursor(None, None)
}

object Readable {
  @SuppressWarnings(Array("org.wartremover.warts.Recursion"))
  private[common] def pullFromCursorWithLimit[F[_], R](
      cursor: Option[String],
      limit: Option[Long],
      get: (Option[String], Option[Long]) => F[ItemsWithCursor[R]]
  ): Pull[F, R, Unit] =
    if (limit.exists(_ <= 0)) {
      Pull.done
    } else {
      Pull.eval(get(cursor, limit)).flatMap { items =>
        Pull.output(Chunk.seq(items.items)) >>
          items.nextCursor
            .map(s => pullFromCursorWithLimit(Some(s), limit.map(_ - items.items.size), get))
            .getOrElse(Pull.done)
      }
    }

  def readWithCursor[F[_], R](
      requestSession: RequestSession[F],
      baseUri: Uri,
      cursor: Option[String],
      limit: Option[Long]
  )(
      implicit itemsWithCursorDecoder: Decoder[ItemsWithCursor[R]]
  ): F[ItemsWithCursor[R]] = {
    implicit val errorOrItemsDecoder: Decoder[Either[CdpApiError, ItemsWithCursor[R]]] =
      EitherDecoder.eitherDecoder[CdpApiError, ItemsWithCursor[R]]
    val uriWithCursor = cursor
      .fold(baseUri)(baseUri.param("cursor", _))
      .param("limit", limit.getOrElse(Resource.defaultLimit).toString)
    requestSession
      .sendCdf { request =>
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
  def retrieveByIds(ids: Seq[Long]): F[Seq[R]]
  def retrieveById(id: Long): F[Option[R]] =
    requestSession.map(retrieveByIds(Seq(id)), (r1: Seq[R]) => r1.headOption)
}

object RetrieveByIds {
  implicit val cogniteIdEncoder: Encoder[CogniteId] = deriveEncoder
  implicit val cogniteIdItemsEncoder: Encoder[Items[CogniteId]] = deriveEncoder

  def retrieveByIds[F[_], R](requestSession: RequestSession[F], baseUri: Uri, ids: Seq[Long])(
      implicit itemsDecoder: Decoder[Items[R]]
  ): F[Seq[R]] = {
    implicit val errorOrItemsDecoder: Decoder[Either[CdpApiError, Items[R]]] =
      EitherDecoder.eitherDecoder[CdpApiError, Items[R]]
    requestSession
      .sendCdf { request =>
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

trait RetrieveByExternalIds[R, F[_]] extends WithRequestSession[F] with BaseUri {
  def retrieveByExternalIds(externalIds: Seq[String]): F[Seq[R]]
  def retrieveByExternalId(externalIds: String): F[Option[R]] =
    requestSession.map(retrieveByExternalIds(Seq(externalIds)), (r1: Seq[R]) => r1.headOption)
}

object RetrieveByExternalIds {
  implicit val cogniteExternalIdEncoder: Encoder[CogniteExternalId] = deriveEncoder
  implicit val cogniteExternalIdItemsEncoder: Encoder[Items[CogniteExternalId]] = deriveEncoder

  def retrieveByExternalIds[F[_], R](
      requestSession: RequestSession[F],
      baseUri: Uri,
      externalIds: Seq[String]
  )(
      implicit itemsDecoder: Decoder[Items[R]]
  ): F[Seq[R]] = {
    implicit val errorOrItemsDecoder: Decoder[Either[CdpApiError, Items[R]]] =
      EitherDecoder.eitherDecoder[CdpApiError, Items[R]]
    requestSession
      .sendCdf { request =>
        request
          .get(uri"$baseUri/byids")
          .body(Items(externalIds.map(CogniteExternalId)))
          .response(asJson[Either[CdpApiError, Items[R]]])
          .mapResponse {
            case Left(value) => throw value.error
            case Right(Left(cdpApiError)) => throw cdpApiError.asException(uri"$baseUri/byids")
            case Right(Right(value)) => value.items
          }
      }
  }
}
