// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.common

import com.cognite.sdk.scala.v1._
import sttp.client3._
import sttp.client3.circe._
import io.circe.{Decoder, Encoder, Json, Printer}
import io.circe.syntax._
import io.circe.generic.semiauto.deriveEncoder
import sttp.model.Uri

final case class UpdateRequest(update: Json, id: Long)
final case class UpdateRequestExternalId(update: Json, externalId: String)

trait UpdateById[R <: ToUpdate[U] with WithId[Long], U, F[_]]
    extends WithRequestSession[F]
    with BaseUrl {
  def updateById(items: Map[Long, U]): F[Seq[R]]

  def updateFromRead(items: Seq[R]): F[Seq[R]] =
    updateById(items.map(a => a.id -> a.toUpdate).toMap)

  def updateOneById(id: Long, item: U): F[R] =
    requestSession.map(
      updateById(Map(id -> item)),
      (r1: Seq[R]) =>
        r1.headOption match {
          case Some(value) => value
          case None => throw SdkException("Unexpected empty response when updating item")
        }
    )

  def updateOneFromRead(item: R): F[R] =
    requestSession.map(
      updateFromRead(Seq(item)),
      (r1: Seq[R]) =>
        r1.headOption match {
          case Some(value) => value
          case None => throw SdkException("Unexpected empty response when updating item")
        }
    )
}

object UpdateById {
  implicit val updateRequestEncoder: Encoder[UpdateRequest] = deriveEncoder
  implicit val updateRequestItemsEncoder: Encoder[Items[UpdateRequest]] = deriveEncoder
  def updateById[F[_], R, U: Encoder](
      requestSession: RequestSession[F],
      baseUrl: Uri,
      updates: Map[Long, U]
  )(implicit decodeReadItems: Decoder[Items[R]]): F[Seq[R]] = {
    require(updates.keys.forall(id => id > 0), "Updating by id requires an id to be set")
    implicit val printer: Printer = Printer.noSpaces.copy(dropNullValues = true)
    requestSession
      .post[Seq[R], Items[R], Items[UpdateRequest]](
        Items(updates.map { case (id, update) =>
          UpdateRequest(update.asJson, id)
        }.toSeq),
        uri"$baseUrl/update",
        value => value.items
      )
  }
}

trait UpdateByExternalId[R, U, F[_]] extends WithRequestSession[F] with BaseUrl {
  def updateByExternalId(items: Map[String, U]): F[Seq[R]]

  def updateOneByExternalId(id: String, item: U): F[R] =
    requestSession.map(
      updateByExternalId(Map(id -> item)),
      (r1: Seq[R]) =>
        r1.headOption match {
          case Some(value) => value
          case None => throw SdkException("Unexpected empty response when updating item")
        }
    )
}

object UpdateByExternalId {
  implicit val updateRequestExternalIdEncoder: Encoder[UpdateRequestExternalId] = deriveEncoder
  implicit val updateRequestExternalIdItemsEncoder: Encoder[Items[UpdateRequestExternalId]] =
    deriveEncoder
  def updateByExternalId[F[_], R, U: Encoder](
      requestSession: RequestSession[F],
      baseUrl: Uri,
      updates: Map[String, U]
  )(implicit decodeReadItems: Decoder[Items[R]]): F[Seq[R]] = {
    require(
      updates.keys.forall(id => id > ""),
      "Updating by externalId requires externalId to be set "
    )
    implicit val printer: Printer = Printer.noSpaces.copy(dropNullValues = true)
    requestSession
      .post[Seq[R], Items[R], Items[UpdateRequestExternalId]](
        Items(updates.map { case (id, update) =>
          UpdateRequestExternalId(update.asJson, id)
        }.toSeq),
        uri"$baseUrl/update",
        value => value.items
      )
  }
}
