// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.common

import com.cognite.sdk.scala.v1._

import com.softwaremill.sttp._
import com.softwaremill.sttp.circe._
import io.circe.{Decoder, Encoder, Json, Printer}
import io.circe.syntax._
import io.circe.derivation.deriveEncoder
import io.scalaland.chimney.Transformer
import io.scalaland.chimney.dsl._

final case class UpdateRequest(update: Json, id: Long)
final case class UpdateRequestExternalId(update: Json, externalId: String)

trait UpdateById[R <: WithId[Long], U, F[_]] extends WithRequestSession[F] with BaseUrl {
  def updateById(items: Map[Long, U]): F[Seq[R]]

  def updateFromRead(items: Seq[R])(
      implicit t: Transformer[R, U]
  ): F[Seq[R]] =
    updateById(items.map(a => a.id -> a.transformInto[U]).toMap)

  def updateOneById(id: Long, item: U): F[R] =
    requestSession.map(
      updateById(Map(id -> item)),
      (r1: Seq[R]) =>
        r1.headOption match {
          case Some(value) => value
          case None => throw SdkException("Unexpected empty response when updating item")
        }
    )

  def updateOneFromRead(item: R)(
      implicit t: Transformer[R, U]
  ): F[R] =
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
  implicit val assetUpdateEncoder: Encoder[AssetUpdate] = deriveEncoder
  implicit val updateRequestEncoder: Encoder[UpdateRequest] = deriveEncoder
  implicit val updateRequestItemsEncoder: Encoder[Items[UpdateRequest]] = deriveEncoder
  def updateById[F[_], R, U: Encoder](
      requestSession: RequestSession[F],
      baseUrl: Uri,
      updates: Map[Long, U]
  )(implicit decodeReadItems: Decoder[Items[R]]): F[Seq[R]] = {
    require(updates.keys.forall(id => id > 0), "Updating by id requires an id to be set")
    implicit val printer: Printer = Printer.noSpaces.copy(dropNullValues = true)
    implicit val errorOrItemsDecoder: Decoder[Either[CdpApiError, Items[R]]] =
      EitherDecoder.eitherDecoder[CdpApiError, Items[R]]
    requestSession
      .post[Seq[R], Items[R], Items[UpdateRequest]](
        Items(updates.map {
          case (id, update) =>
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
  implicit val assetUpdateEncoder: Encoder[AssetUpdate] = deriveEncoder
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
    implicit val errorOrItemsDecoder: Decoder[Either[CdpApiError, Items[R]]] =
      EitherDecoder.eitherDecoder[CdpApiError, Items[R]]
    requestSession
      .post[Seq[R], Items[R], Items[UpdateRequestExternalId]](
        Items(updates.map {
          case (id, update) =>
            UpdateRequestExternalId(update.asJson, id)
        }.toSeq),
        uri"$baseUrl/update",
        value => value.items
      )
  }
}
