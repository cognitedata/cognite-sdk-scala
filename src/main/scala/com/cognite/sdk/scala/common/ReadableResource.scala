// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.common

import cats.effect.Concurrent
import com.cognite.sdk.scala.v1._
import com.softwaremill.sttp._
import com.softwaremill.sttp.circe._
import fs2._
import io.circe.Decoder

// TODO: Verify that index and numPartitions are valid
final case class Partition(index: Int = 1, numPartitions: Int = 1) {
  override def toString: String = s"${index.toString}/${numPartitions.toString}"
}

trait Readable[R, F[_]] extends WithRequestSession[F] with BaseUrl {
  private[sdk] def readWithCursor(
      cursor: Option[String],
      limit: Option[Int],
      partition: Option[Partition]
  ): F[ItemsWithCursor[R]]
  def read(limit: Option[Int] = None): F[ItemsWithCursor[R]] =
    readWithCursor(None, limit, None)

  private[sdk] def listWithNextCursor(
      cursor: Option[String],
      limit: Option[Int]
  ): Stream[F, R] =
    Readable
      .pullFromCursor(cursor, limit, None, readWithCursor)
      .stream

  def list(limit: Option[Int] = None): Stream[F, R] =
    listWithNextCursor(None, limit)
}

trait PartitionedReadable[R, F[_]] extends Readable[R, F] {
  def listPartitions(numPartitions: Int, limitPerPartition: Option[Int] = None): Seq[Stream[F, R]] =
    1.to(numPartitions).map { i =>
      Readable
        .pullFromCursor(
          None,
          limitPerPartition,
          Some(Partition(i, numPartitions)),
          readWithCursor
        )
        .stream
    }

  def listConcurrently(numPartitions: Int, limitPerPartition: Option[Int] = None)(implicit
      c: Concurrent[F]
  ): Stream[F, R] =
    listPartitions(numPartitions, limitPerPartition).fold(Stream.empty)(_.merge(_))
}

object Readable {
  @SuppressWarnings(Array("org.wartremover.warts.Recursion"))
  private[sdk] def pullFromCursor[F[_], R](
      cursor: Option[String],
      maxItemsReturned: Option[Int],
      partition: Option[Partition],
      get: (Option[String], Option[Int], Option[Partition]) => F[ItemsWithCursor[R]]
  ): Pull[F, R, Unit] =
    if (maxItemsReturned.exists(_ <= 0)) {
      Pull.done
    } else {
      Pull.eval(get(cursor, maxItemsReturned, partition)).flatMap { items =>
        Pull.output(Chunk.seq(items.items)) >>
          items.nextCursor
            .map { s =>
              pullFromCursor(Some(s), maxItemsReturned.map(_ - items.items.size), partition, get)
            }
            .getOrElse(Pull.done)
      }
    }

  @SuppressWarnings(Array("org.wartremover.warts.Recursion"))
  private[sdk] def pageThroughCursors[F[_], State, Resp <: ResponseWithCursor](
      cursor: Option[String],
      state: State,
      get: (Option[String], State) => F[Option[(Resp, State)]]
  ): Pull[F, Resp, Unit] =
    Pull.eval(get(cursor, state)).flatMap {
      case None => Pull.done
      case Some((response, state)) =>
        Pull.output1(response) >>
          response.nextCursor
            .map(cursor => pageThroughCursors(Some(cursor), state, get))
            .getOrElse(Pull.done)
    }

  private def uriWithCursorAndLimit(
      baseUrl: Uri,
      cursor: Option[String],
      limit: Option[Int],
      batchSize: Int
  ) = {
    val uriWithCursor = cursor.fold(baseUrl)(baseUrl.param("cursor", _))
    val l = limit.getOrElse(batchSize)
    uriWithCursor.param("limit", math.min(l, batchSize).toString)
  }

  private[sdk] def readWithCursor[F[_], R](
      requestSession: RequestSession[F],
      baseUrl: Uri,
      cursor: Option[String],
      maxItemsReturned: Option[Int],
      partition: Option[Partition],
      batchSize: Int
  )(implicit
      itemsWithCursorDecoder: Decoder[ItemsWithCursor[R]]
  ): F[ItemsWithCursor[R]] = {
    val uriWithCursor = uriWithCursorAndLimit(baseUrl, cursor, maxItemsReturned, batchSize)
    val uriWithCursorAndPartition = partition.fold(uriWithCursor) { p =>
      uriWithCursor.param("partition", p.toString)
    }

    readSimple(requestSession, uriWithCursorAndPartition)
  }

  private[sdk] def readSimple[F[_], R](
      requestSession: RequestSession[F],
      uri: Uri
  )(implicit
      itemsWithCursorDecoder: Decoder[ItemsWithCursor[R]]
  ): F[ItemsWithCursor[R]] = {
    implicit val errorOrItemsDecoder: Decoder[Either[CdpApiError, ItemsWithCursor[R]]] =
      EitherDecoder.eitherDecoder[CdpApiError, ItemsWithCursor[R]]

    requestSession.get[ItemsWithCursor[R], ItemsWithCursor[R]](
      uri,
      value => value
    )
  }
}

trait RetrieveByIds[R, F[_]] extends WithRequestSession[F] with BaseUrl {
  def retrieveByIds(ids: Seq[Long]): F[Seq[R]]
  @SuppressWarnings(Array("org.wartremover.warts.TraversableOps"))
  def retrieveById(id: Long): F[R] =
    // The API returns an error causing an exception to be thrown if the item isn't found,
    // so .head is safe here.
    requestSession.map(retrieveByIds(Seq(id)), (r1: Seq[R]) => r1.head)
}

object RetrieveByIds {
  def retrieveByIds[F[_], R](requestSession: RequestSession[F], baseUrl: Uri, ids: Seq[Long])(
      implicit itemsDecoder: Decoder[Items[R]]
  ): F[Seq[R]] = {
    implicit val errorOrItemsDecoder: Decoder[Either[CdpApiError, Items[R]]] =
      EitherDecoder.eitherDecoder[CdpApiError, Items[R]]
    requestSession.post[Seq[R], Items[R], Items[CogniteInternalId]](
      Items(ids.map(CogniteInternalId)),
      uri"$baseUrl/byids",
      value => value.items
    )
  }
}

trait RetrieveByIdsWithIgnoreUnknownIds[R, F[_]] extends RetrieveByIds[R, F] {
  override def retrieveByIds(ids: Seq[Long]): F[Seq[R]] =
    retrieveByIds(ids, ignoreUnknownIds = false)
  def retrieveByIds(ids: Seq[Long], ignoreUnknownIds: Boolean): F[Seq[R]]
}

object RetrieveByIdsWithIgnoreUnknownIds {
  def retrieveByIds[F[_], R](
      requestSession: RequestSession[F],
      baseUrl: Uri,
      ids: Seq[Long],
      ignoreUnknownIds: Boolean
  )(implicit
      itemsDecoder: Decoder[Items[R]]
  ): F[Seq[R]] = {
    implicit val errorOrItemsDecoder: Decoder[Either[CdpApiError, Items[R]]] =
      EitherDecoder.eitherDecoder[CdpApiError, Items[R]]
    requestSession.post[Seq[R], Items[R], ItemsWithIgnoreUnknownIds[CogniteId]](
      ItemsWithIgnoreUnknownIds(ids.map(CogniteInternalId), ignoreUnknownIds),
      uri"$baseUrl/byids",
      value => value.items
    )
  }
}

trait RetrieveByExternalIds[R, F[_]] extends WithRequestSession[F] with BaseUrl {
  def retrieveByExternalIds(externalIds: Seq[String]): F[Seq[R]]
  @SuppressWarnings(Array("org.wartremover.warts.TraversableOps"))
  def retrieveByExternalId(externalId: String): F[R] =
    // The API returns an error causing an exception to be thrown if the item isn't found,
    // so .head is safe here.
    requestSession.map(retrieveByExternalIds(Seq(externalId)), (r1: Seq[R]) => r1.head)
}

object RetrieveByExternalIds {
  def retrieveByExternalIds[F[_], R](
      requestSession: RequestSession[F],
      baseUrl: Uri,
      externalIds: Seq[String]
  )(implicit
      itemsDecoder: Decoder[Items[R]]
  ): F[Seq[R]] = {
    implicit val errorOrItemsDecoder: Decoder[Either[CdpApiError, Items[R]]] =
      EitherDecoder.eitherDecoder[CdpApiError, Items[R]]
    requestSession.post[Seq[R], Items[R], Items[CogniteExternalId]](
      Items(externalIds.map(CogniteExternalId)),
      uri"$baseUrl/byids",
      value => value.items
    )
  }
}

trait RetrieveByExternalIdsWithIgnoreUnknownIds[R, F[_]] extends RetrieveByExternalIds[R, F] {
  override def retrieveByExternalIds(ids: Seq[String]): F[Seq[R]] =
    retrieveByExternalIds(ids, ignoreUnknownIds = false)
  def retrieveByExternalIds(ids: Seq[String], ignoreUnknownIds: Boolean): F[Seq[R]]
}

object RetrieveByExternalIdsWithIgnoreUnknownIds {
  def retrieveByExternalIds[F[_], R](
      requestSession: RequestSession[F],
      baseUrl: Uri,
      externalIds: Seq[String],
      ignoreUnknownIds: Boolean
  )(implicit
      itemsDecoder: Decoder[Items[R]]
  ): F[Seq[R]] = {
    implicit val errorOrItemsDecoder: Decoder[Either[CdpApiError, Items[R]]] =
      EitherDecoder.eitherDecoder[CdpApiError, Items[R]]
    requestSession.post[Seq[R], Items[R], ItemsWithIgnoreUnknownIds[CogniteId]](
      ItemsWithIgnoreUnknownIds(externalIds.map(CogniteExternalId), ignoreUnknownIds),
      uri"$baseUrl/byids",
      value => value.items
    )
  }
}
