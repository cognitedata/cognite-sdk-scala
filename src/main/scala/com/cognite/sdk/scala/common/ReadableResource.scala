package com.cognite.sdk.scala.common

import cats.effect.Concurrent
import com.cognite.sdk.scala.v1._
import com.softwaremill.sttp._
import com.softwaremill.sttp.circe._
import fs2._
import io.circe.{Decoder, Encoder}
import io.circe.derivation.deriveEncoder

// TODO: Verify that index and numPartitions are valid
final case class Partition(index: Int = 1, numPartitions: Int = 1) {
  override def toString: String = s"${index.toString}/${numPartitions.toString}"
}

trait Readable[R, F[_]] extends WithRequestSession[F] with BaseUri {
  private[sdk] def readWithCursor(
      cursor: Option[String],
      limit: Option[Long],
      partition: Option[Partition]
  ): F[ItemsWithCursor[R]]

  def readFromCursor(cursor: String): F[ItemsWithCursor[R]] =
    readWithCursor(Some(cursor), None, None)

  def readFromCursorWithLimit(
      cursor: String,
      limit: Long
  ): F[ItemsWithCursor[R]] =
    readWithCursor(Some(cursor), Some(limit), None)

  def read(): F[ItemsWithCursor[R]] =
    readWithCursor(None, None, None)

  def readWithLimit(limit: Long): F[ItemsWithCursor[R]] =
    readWithCursor(None, Some(limit), None)

  private[sdk] def listWithNextCursor(
      cursor: Option[String],
      limit: Option[Long]
  ): Stream[F, R] =
    Readable
      .pullFromCursorWithLimit(cursor, limit, None, readWithCursor)
      .stream

  def listFromCursor(cursor: String): Stream[F, R] =
    listWithNextCursor(Some(cursor), None)

  def listWithLimit(limit: Long): Stream[F, R] =
    listWithNextCursor(None, Some(limit))

  def listFromCursorWithLimit(
      cursor: String,
      limit: Long
  ): Stream[F, R] =
    listWithNextCursor(Some(cursor), Some(limit))

  def list(): Stream[F, R] =
    listWithNextCursor(None, None)
}

trait PartitionedReadable[R, F[_]] extends Readable[R, F] {
  private def listPartitionsMaybeWithLimit(numPartitions: Int, limitPerPartition: Option[Long]) =
    1.to(numPartitions).map { i =>
      Readable
        .pullFromCursorWithLimit(
          None,
          limitPerPartition,
          Some(Partition(i, numPartitions)),
          readWithCursor
        )
        .stream
    }

  def listPartitions(numPartitions: Int): Seq[Stream[F, R]] =
    listPartitionsMaybeWithLimit(numPartitions, None)

  def listPartitionsWithLimit(numPartitions: Int, limitPerPartition: Long): Seq[Stream[F, R]] =
    listPartitionsMaybeWithLimit(numPartitions, Some(limitPerPartition))

  def listConcurrently(numPartitions: Int)(implicit c: Concurrent[F]): Stream[F, R] =
    listPartitions(numPartitions).fold(Stream.empty)(_.merge(_))

  def listConcurrentlyWithLimit(numPartitions: Int, limitPerPartition: Long)(
      implicit c: Concurrent[F]
  ): Stream[F, R] =
    listPartitionsWithLimit(numPartitions, limitPerPartition).fold(Stream.empty)(_.merge(_))
}

object Readable {
  @SuppressWarnings(Array("org.wartremover.warts.Recursion"))
  private[sdk] def pullFromCursorWithLimit[F[_], R](
      cursor: Option[String],
      limit: Option[Long],
      partition: Option[Partition],
      get: (Option[String], Option[Long], Option[Partition]) => F[ItemsWithCursor[R]]
  ): Pull[F, R, Unit] =
    if (limit.exists(_ <= 0)) {
      Pull.done
    } else {
      Pull.eval(get(cursor, limit, partition)).flatMap { items =>
        Pull.output(Chunk.seq(items.items)) >>
          items.nextCursor
            .map { s =>
              pullFromCursorWithLimit(Some(s), limit.map(_ - items.items.size), partition, get)
            }
            .getOrElse(Pull.done)
      }
    }

  private def uriWithCursorAndLimit(baseUri: Uri, cursor: Option[String], limit: Option[Long]) =
    cursor
      .fold(baseUri)(baseUri.param("cursor", _))
      .param("limit", limit.getOrElse(Resource.defaultLimit).toString)

  private[sdk] def readWithCursor[F[_], R](
      requestSession: RequestSession[F],
      baseUri: Uri,
      cursor: Option[String],
      limit: Option[Long],
      partition: Option[Partition]
  )(
      implicit itemsWithCursorDecoder: Decoder[ItemsWithCursor[R]]
  ): F[ItemsWithCursor[R]] = {
    implicit val errorOrItemsDecoder: Decoder[Either[CdpApiError, ItemsWithCursor[R]]] =
      EitherDecoder.eitherDecoder[CdpApiError, ItemsWithCursor[R]]
    val uriWithCursor = uriWithCursorAndLimit(baseUri, cursor, limit)
    val uriWithCursorAndPartition = partition.fold(uriWithCursor) { p =>
      uriWithCursor.param("partition", p.toString)
    }

    requestSession
      .sendCdf { request =>
        request
          .get(uriWithCursorAndPartition)
          .response(asJson[Either[CdpApiError, ItemsWithCursor[R]]])
          .mapResponse {
            case Left(value) => throw value.error
            case Right(Left(cdpApiError)) =>
              throw cdpApiError.asException(uriWithCursorAndPartition)
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
          .post(uri"$baseUri/byids")
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
          .post(uri"$baseUri/byids")
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
