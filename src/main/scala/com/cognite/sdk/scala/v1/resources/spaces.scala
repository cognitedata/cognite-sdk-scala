// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1.resources

import com.cognite.sdk.scala.common._
import com.cognite.sdk.scala.v1._
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder, Printer}
import sttp.client3._
import sttp.client3.circe._

class Spaces[F[_]](val requestSession: RequestSession[F])
    extends WithRequestSession[F]
    with BaseUrl {
  import Spaces._
  override val baseUrl = uri"${requestSession.baseUrl}/models/spaces"

  def createItems(spaces: Seq[SpaceCreate]): F[Seq[Space]] = {
    implicit val printer: Printer = Printer.noSpaces.copy(dropNullValues = true)
    requestSession.post[Seq[Space], Items[Space], Items[SpaceCreate]](
      Items(spaces),
      uri"$baseUrl",
      value => value.items
    )
  }

  def deleteItems(spaces: Seq[String]): F[Unit] = {
    implicit val printer: Printer = Printer.noSpaces.copy(dropNullValues = true)
    requestSession.post[Unit, Unit, Items[SpaceId]](
      Items(spaces.map(SpaceId(_))),
      uri"$baseUrl/delete",
      _ => ()
    )
  }

  def retrieveBySpaceIds(spaces: Seq[String]): F[Seq[Space]] = {
    implicit val printer: Printer = Printer.noSpaces.copy(dropNullValues = true)
    requestSession
      .post[Seq[Space], Items[Space], Items[SpaceId]](
        Items(spaces.map(SpaceId(_))),
        uri"$baseUrl/byids",
        value => value.items
      )
  }

}

object Spaces {
  implicit val spaceCreateEncoder: Encoder[SpaceCreate] = deriveEncoder[SpaceCreate]
  implicit val spaceCreateItemsEncoder: Encoder[Items[SpaceCreate]] =
    deriveEncoder[Items[SpaceCreate]]

  implicit val spaceIdEncoder: Encoder[SpaceId] = deriveEncoder[SpaceId]
  implicit val spaceIdItemsEncoder: Encoder[Items[SpaceId]] = deriveEncoder[Items[SpaceId]]

  implicit val spaceEncoder: Encoder[Space] = deriveEncoder[Space]
  implicit val spaceItemsEncoder: Encoder[Items[Space]] = deriveEncoder[Items[Space]]

  implicit val spaceDecoder: Decoder[Space] = deriveDecoder[Space]
  implicit val spaceItemsDecoder: Decoder[Items[Space]] = deriveDecoder[Items[Space]]
}
