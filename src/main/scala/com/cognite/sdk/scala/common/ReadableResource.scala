// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.common

import com.cognite.sdk.scala.v1._
import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec
import sttp.client3._
import sttp.client3.jsoniter_scala._
import sttp.model.Uri

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
}

object Readable {
  private def uriWithCursorAndLimit(
      baseUrl: Uri,
      cursor: Option[String],
      limit: Option[Int],
      batchSize: Int
  ) = {
    val uriWithCursor = cursor.fold(baseUrl)(baseUrl.addParam("cursor", _))
    val l = limit.getOrElse(batchSize)
    uriWithCursor.addParam("limit", math.min(l, batchSize).toString)
  }

  private[sdk] def readWithCursor[F[_], R](
      requestSession: RequestSession[F],
      baseUrl: Uri,
      cursor: Option[String],
      maxItemsReturned: Option[Int],
      partition: Option[Partition],
      batchSize: Int
  )(implicit itemsWithCursorCodec: JsonValueCodec[ItemsWithCursor[R]]): F[ItemsWithCursor[R]] = {
    val uriWithCursor = uriWithCursorAndLimit(baseUrl, cursor, maxItemsReturned, batchSize)
    val uriWithCursorAndPartition = partition.fold(uriWithCursor) { p =>
      uriWithCursor.addParam("partition", p.toString)
    }

    readSimple(requestSession, uriWithCursorAndPartition)
  }

  private[sdk] def readSimple[F[_], R](
      requestSession: RequestSession[F],
      uri: Uri
  )(implicit itemsWithCursorCodec: JsonValueCodec[ItemsWithCursor[R]]): F[ItemsWithCursor[R]] =
    requestSession.get[ItemsWithCursor[R], ItemsWithCursor[R]](
      uri,
      value => value
    )
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
      implicit itemsCodec: JsonValueCodec[Items[R]]
  ): F[Seq[R]] =
    requestSession.post[Seq[R], Items[R], Items[CogniteInternalId]](
      Items(ids.map(CogniteInternalId.apply)),
      uri"$baseUrl/byids",
      value => value.items
    )
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
  )(implicit itemsCodec: JsonValueCodec[Items[R]]): F[Seq[R]] =
    requestSession.post[Seq[R], Items[R], ItemsWithIgnoreUnknownIds[CogniteId]](
      ItemsWithIgnoreUnknownIds(ids.map(CogniteInternalId.apply), ignoreUnknownIds),
      uri"$baseUrl/byids",
      value => value.items
    )
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
  )(implicit itemsCodec: JsonValueCodec[Items[R]]): F[Seq[R]] =
    requestSession.post[Seq[R], Items[R], Items[CogniteExternalId]](
      Items(externalIds.map(CogniteExternalId.apply)),
      uri"$baseUrl/byids",
      value => value.items
    )
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
  )(implicit itemsCodec: JsonValueCodec[Items[R]]): F[Seq[R]] =
    requestSession.post[Seq[R], Items[R], ItemsWithIgnoreUnknownIds[CogniteId]](
      ItemsWithIgnoreUnknownIds(externalIds.map(CogniteExternalId.apply), ignoreUnknownIds),
      uri"$baseUrl/byids",
      value => value.items
    )
}
