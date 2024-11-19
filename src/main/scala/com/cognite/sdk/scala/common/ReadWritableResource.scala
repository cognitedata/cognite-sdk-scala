// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.common

import com.cognite.sdk.scala.v1._
import sttp.client3._
import sttp.client3.circe._
import io.circe.{Decoder, Encoder}
import sttp.model.Uri

trait DeleteByIds[F[_], PrimitiveId] {
  def deleteByIds(ids: Seq[PrimitiveId]): F[Unit]

  def deleteById(id: PrimitiveId): F[Unit] = deleteByIds(Seq(id))
}

trait DeleteByIdsWithIgnoreUnknownIds[F[_], PrimitiveId] extends DeleteByIds[F, PrimitiveId] {
  def deleteByIds(ids: Seq[PrimitiveId], ignoreUnknownIds: Boolean = false): F[Unit]

  def deleteById(id: PrimitiveId, ignoreUnknownIds: Boolean = false): F[Unit] =
    deleteByIds(Seq(id), ignoreUnknownIds)
}

object DeleteByIds {
  def deleteByIds[F[_]](
      requestSession: RequestSession[F],
      baseUrl: Uri,
      ids: Seq[Long]
  ): F[Unit] =
    // TODO: group deletes by max deletion request size
    //       or assert that length of `ids` is less than max deletion request size
    requestSession.post[Unit, Unit, Items[CogniteInternalId]](
      Items(ids.map(CogniteInternalId.apply)),
      uri"$baseUrl/delete",
      _ => ()
    )
}

trait DeleteByExternalIds[F[_]] {
  def deleteByExternalIds(externalIds: Seq[String]): F[Unit]

  def deleteByExternalId(externalId: String): F[Unit] = deleteByExternalIds(Seq(externalId))
}

trait DeleteByInstanceIds[F[_]] {
  def deleteByInstanceIds(instanceIds: Seq[InstanceId]): F[Unit]

  def deleteByInstanceId(instanceId: InstanceId): F[Unit] = deleteByInstanceIds(Seq(instanceId))
}

trait DeleteByExternalIdsWithIgnoreUnknownIds[F[_]] extends DeleteByExternalIds[F] {
  def deleteByExternalIds(externalIds: Seq[String], ignoreUnknownIds: Boolean = false): F[Unit]

  def deleteByExternalId(externalId: String, ignoreUnknownIds: Boolean = false): F[Unit] =
    deleteByExternalIds(Seq(externalId), ignoreUnknownIds)
}

object DeleteByExternalIds {
  implicit val errorOrUnitDecoder: Decoder[Either[CdpApiError, Unit]] =
    EitherDecoder.eitherDecoder[CdpApiError, Unit]

  def deleteByExternalIds[F[_]](
      requestSession: RequestSession[F],
      baseUrl: Uri,
      externalIds: Seq[String]
  ): F[Unit] =
    // TODO: group deletes by max deletion request size
    //       or assert that length of `ids` is less than max deletion request size
    requestSession.post[Unit, Unit, Items[CogniteExternalId]](
      Items(externalIds.map(CogniteExternalId.apply)),
      uri"$baseUrl/delete",
      _ => ()
    )
}

//todo generify using CogniteId/CogniteIdWithInstanceId?
object DeleteByInstanceIds {
  //TODO generify?
  implicit val errorOrUnitDecoder: Decoder[Either[CdpApiError, Unit]] =
    EitherDecoder.eitherDecoder[CdpApiError, Unit]

  def deleteByInstanceIds[F[_]](
      requestSession: RequestSession[F],
      baseUrl: Uri,
      instanceIds: Seq[InstanceId]
  ): F[Unit] =
  // TODO: group deletes by max deletion request size
  //       or assert that length of `ids` is less than max deletion request size
  requestSession.post[Unit, Unit, Items[InstanceId]](
    Items(instanceIds),
    uri"$baseUrl/delete",
    _ => ()
  )
}

trait DeleteByCogniteIds[F[_]]
    extends DeleteByIdsWithIgnoreUnknownIds[F, Long]
    with DeleteByExternalIdsWithIgnoreUnknownIds[F] {
  def delete(ids: Seq[CogniteId], ignoreUnknownIds: Boolean = false): F[Unit]

  def deleteByIds(ids: Seq[Long]): F[Unit] = delete(ids.map(CogniteInternalId(_)), false)

  def deleteByIds(ids: Seq[Long], ignoreUnknownIds: Boolean = false): F[Unit] =
    delete(ids.map(CogniteInternalId(_)), ignoreUnknownIds)

  def deleteByExternalIds(externalIds: Seq[String]): F[Unit] =
    delete(externalIds.map(CogniteExternalId(_)), false)

  def deleteByExternalIds(externalIds: Seq[String], ignoreUnknownIds: Boolean = false): F[Unit] =
    delete(externalIds.map(CogniteExternalId(_)), ignoreUnknownIds)
}

object DeleteByCogniteIds {
  def deleteWithIgnoreUnknownIds[F[_]](
      requestSession: RequestSession[F],
      baseUrl: Uri,
      ids: Seq[CogniteId],
      ignoreUnknownIds: Boolean
  ): F[Unit] =
    requestSession.post[Unit, Unit, ItemsWithIgnoreUnknownIds[CogniteId]](
      ItemsWithIgnoreUnknownIds(ids, ignoreUnknownIds),
      uri"$baseUrl/delete",
      _ => ()
    )
}

trait Create[R <: ToCreate[W], W, F[_]]
    extends WithRequestSession[F]
    with CreateOne[R, W, F]
    with BaseUrl {
  def createItems(items: Items[W]): F[Seq[R]]

  def create(items: Seq[W]): F[Seq[R]] =
    createItems(Items(items))

  def createFromRead(items: Seq[R]): F[Seq[R]] =
    createItems(Items(items.map(_.toCreate)))

  def createOne(item: W): F[R] =
    requestSession.map(
      create(Seq(item)),
      (r1: Seq[R]) =>
        r1.headOption match {
          case Some(value) => value
          case None => throw SdkException("Unexpected empty response when creating item")
        }
    )
}

object Create {
  def createItems[F[_], R, W](requestSession: RequestSession[F], baseUrl: Uri, items: Items[W])(
      implicit readDecoder: Decoder[ItemsWithCursor[R]],
      itemsEncoder: Encoder[Items[W]]
  ): F[Seq[R]] =
    requestSession.post[Seq[R], ItemsWithCursor[R], Items[W]](
      items,
      baseUrl,
      value => value.items
    )
}

trait CreateOne[R <: ToCreate[W], W, F[_]] extends WithRequestSession[F] with BaseUrl {
  def createOne(item: W): F[R]

  def createOneFromRead(item: R): F[R] = createOne(item.toCreate)
}

object CreateOne {
  def createOne[F[_], R, W](requestSession: RequestSession[F], baseUrl: Uri, item: W)(
      implicit readDecoder: Decoder[R],
      itemsEncoder: Encoder[W]
  ): F[R] =
    requestSession.post[R, R, W](
      item,
      baseUrl,
      value => value
    )
}
