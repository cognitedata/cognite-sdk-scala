// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1.resources

import com.cognite.sdk.scala.common._
import com.cognite.sdk.scala.v1._
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder, Printer}
import sttp.client3._
import sttp.client3.circe._

class SpacesV3[F[_]](val requestSession: RequestSession[F])
    extends WithRequestSession[F]
    with BaseUrl {
  import SpacesV3._
  override val baseUrl = uri"${requestSession.baseUrl}/models/spaces"

  def createItems(spaces: Seq[SpaceCreateDefinition]): F[Seq[SpaceDefinition]] = {
    implicit val printer: Printer = Printer.noSpaces.copy(dropNullValues = true)
    requestSession.post[Seq[SpaceDefinition], Items[SpaceDefinition], Items[SpaceCreateDefinition]](
      Items(spaces),
      uri"$baseUrl",
      value => value.items
    )
  }

  def deleteItems(spaces: Seq[SpaceById]): F[Unit] =
    requestSession.post[Unit, Unit, Items[SpaceById]](
      Items(spaces),
      uri"$baseUrl/delete",
      _ => ()
    )

  def retrieveItems(
      spaceIds: Seq[SpaceById]
  ): F[Seq[SpaceDefinition]] =
    requestSession
      .post[Seq[SpaceDefinition], Items[SpaceDefinition], Items[SpaceById]](
        Items(
          spaceIds
        ),
        uri"$baseUrl/byids",
        value => value.items
      )

}

object SpacesV3 {
  implicit val spaceCreateDefinitionEncoder: Encoder[SpaceCreateDefinition] =
    deriveEncoder[SpaceCreateDefinition]
  implicit val spaceCreateDefitinitionItemsEncoder: Encoder[Items[SpaceCreateDefinition]] =
    deriveEncoder[Items[SpaceCreateDefinition]]
  implicit val spaceByIdEncoder: Encoder[SpaceById] =
    deriveEncoder[SpaceById]
  implicit val spaceByIdItemsEncoder: Encoder[Items[SpaceById]] =
    deriveEncoder[Items[SpaceById]]
  implicit val spaceDefinitionDecoder: Decoder[SpaceDefinition] =
    deriveDecoder[SpaceDefinition]
  implicit val spaceDefitinitionItemsDecoder: Decoder[Items[SpaceDefinition]] =
    deriveDecoder[Items[SpaceDefinition]]
}
