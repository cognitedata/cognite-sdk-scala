// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1.resources

import com.cognite.sdk.scala.common._
import com.cognite.sdk.scala.v1._
import io.circe.generic.semiauto.deriveDecoder
import io.circe.{Decoder, Printer}
import sttp.client3._
import sttp.client3.circe._

@deprecated("message", since = "0")
class Spaces[F[_]](val requestSession: RequestSession[F])
    extends WithRequestSession[F]
    with BaseUrl {
  import Spaces._
  override val baseUrl = uri"${requestSession.baseUrl}/datamodelstorage/spaces"

  def createItems(externalIds: Seq[String]): F[Seq[String]] = {
    implicit val printer: Printer = Printer.noSpaces.copy(dropNullValues = true)
    requestSession.post[Seq[String], Items[CogniteExternalId], Items[CogniteId]](
      Items(externalIds.map(CogniteExternalId(_))),
      uri"$baseUrl",
      value => value.items.map(_.externalId)
    )
  }

  def deleteItems(externalIds: Seq[String]): F[Unit] =
    requestSession.post[Unit, Unit, Items[CogniteId]](
      Items(externalIds.map(CogniteExternalId(_))),
      uri"$baseUrl/delete",
      _ => ()
    )

  def list(): F[Seq[String]] =
    requestSession.post[Seq[String], Items[CogniteExternalId], Unit](
      (),
      uri"$baseUrl/list",
      value => value.items.map(_.externalId)
    )

  def retrieveByExternalIds(
      externalIds: Seq[String]
  ): F[Seq[String]] =
    requestSession
      .post[Seq[String], Items[CogniteExternalId], Items[CogniteId]](
        Items(
          externalIds.map(CogniteExternalId(_))
        ),
        uri"$baseUrl/byids",
        value => value.items.map(_.externalId)
      )

}

@deprecated("message", since = "0")
object Spaces {
  implicit val externalIdItemsDecoder: Decoder[Items[CogniteExternalId]] =
    deriveDecoder[Items[CogniteExternalId]]
}
