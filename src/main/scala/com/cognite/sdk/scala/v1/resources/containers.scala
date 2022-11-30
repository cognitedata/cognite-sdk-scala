// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1.resources

import com.cognite.sdk.scala.common._
import com.cognite.sdk.scala.v1._
import com.cognite.sdk.scala.v1.containers._
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder, Printer}
import sttp.client3._
import sttp.client3.circe._

class Containers[F[_]](val requestSession: RequestSession[F])
    extends WithRequestSession[F]
    with BaseUrl {
  implicit val nullDroppingPrinter: Printer = Printer.noSpaces.copy(dropNullValues = true)
  override val baseUrl = uri"${requestSession.baseUrl}/models/containers"
  import Containers._

  def createItems(containers: Seq[ContainerCreate]): F[Seq[ContainerRead]] =
    requestSession.post[Seq[ContainerRead], Items[ContainerRead], Items[ContainerCreate]](
      Items(items = containers),
      uri"$baseUrl",
      value => value.items
    )

  def list(): F[Seq[ContainerRead]] =
    requestSession.get[Seq[ContainerRead], Items[ContainerRead]](
      uri"$baseUrl",
      value => value.items
    )

  def retrieveByExternalIds(containersRefs: Seq[ContainerId]): F[Seq[ContainerRead]] =
    requestSession.post[Seq[ContainerRead], Items[ContainerRead], Items[ContainerId]](
      Items(items = containersRefs),
      uri"$baseUrl/byids",
      value => value.items
    )

  def delete(containersRefs: Seq[ContainerId]): F[Unit] =
    requestSession.post[Unit, Items[ContainerCreate], Items[ContainerId]](
      Items(items = containersRefs),
      uri"$baseUrl/delete",
      _ => ()
    )
}

object Containers {
  implicit val containerPropertyDefinitionEncoder: Encoder[ContainerPropertyDefinition] =
    deriveEncoder[ContainerPropertyDefinition]

  implicit val containerIdEncoder: Encoder[ContainerId] = deriveEncoder[ContainerId]

  implicit val containerIdItemsEncoder: Encoder[Items[ContainerId]] =
    deriveEncoder[Items[ContainerId]]

  implicit val containerPropertyDefinitionDecoder: Decoder[ContainerPropertyDefinition] =
    deriveDecoder[ContainerPropertyDefinition]

  implicit val containerCreateEncoder: Encoder[ContainerCreate] = deriveEncoder[ContainerCreate]

  implicit val containerCreateDecoder: Decoder[ContainerCreate] = deriveDecoder[ContainerCreate]

  implicit val containerCreateItemsEncoder: Encoder[Items[ContainerCreate]] =
    deriveEncoder[Items[ContainerCreate]]

  implicit val containerCreateItemsDecoder: Decoder[Items[ContainerCreate]] =
    deriveDecoder[Items[ContainerCreate]]

  implicit val containerReferenceItemsEncoder: Encoder[Items[ContainerReference]] =
    deriveEncoder[Items[ContainerReference]]

  implicit val containerReferenceItemsDecoder: Decoder[Items[ContainerReference]] =
    deriveDecoder[Items[ContainerReference]]

  implicit val containerReadEncoder: Encoder[ContainerRead] = deriveEncoder[ContainerRead]

  implicit val containerReadDecoder: Decoder[ContainerRead] = deriveDecoder[ContainerRead]

  implicit val containerReadItemsEncoder: Encoder[Items[ContainerRead]] =
    deriveEncoder[Items[ContainerRead]]

  implicit val containerReadItemsDecoder: Decoder[Items[ContainerRead]] =
    deriveDecoder[Items[ContainerRead]]
}
