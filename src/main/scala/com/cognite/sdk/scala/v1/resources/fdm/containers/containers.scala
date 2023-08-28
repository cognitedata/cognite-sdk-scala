// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1.resources.fdm.containers

import com.cognite.sdk.scala.common._
import com.cognite.sdk.scala.v1._
import com.cognite.sdk.scala.v1.fdm.containers._
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder, Printer}
import sttp.client3._
import sttp.client3.circe._

@deprecated("message", since = "0")
class Containers[F[_]](val requestSession: RequestSession[F])
    extends WithRequestSession[F]
    with BaseUrl {
  implicit val nullDroppingPrinter: Printer = Printer.noSpaces.copy(dropNullValues = true)
  override val baseUrl = uri"${requestSession.baseUrl}/models/containers"
  import Containers._

  def createItems(containers: Seq[ContainerCreateDefinition]): F[Seq[ContainerDefinition]] =
    requestSession
      .post[Seq[ContainerDefinition], Items[ContainerDefinition], Items[ContainerCreateDefinition]](
        Items(items = containers),
        uri"$baseUrl",
        value => value.items
      )

  def list(space: String, limit: Int = 10): F[Seq[ContainerDefinition]] =
    requestSession.get[Seq[ContainerDefinition], Items[ContainerDefinition]](
      uri"$baseUrl".addParams(Map("space" -> space, "limit" -> limit.toString)),
      value => value.items
    )

  def retrieveByExternalIds(containersRefs: Seq[ContainerId]): F[Seq[ContainerDefinition]] =
    requestSession.post[Seq[ContainerDefinition], Items[ContainerDefinition], Items[ContainerId]](
      Items(items = containersRefs),
      uri"$baseUrl/byids",
      value => value.items
    )

  def delete(containersRefs: Seq[ContainerId]): F[Seq[ContainerId]] =
    requestSession.post[Seq[ContainerId], Items[ContainerId], Items[ContainerId]](
      Items(items = containersRefs),
      uri"$baseUrl/delete",
      value => value.items
    )
}

@deprecated("message", since = "0")
object Containers {
  implicit val containerIdEncoder: Encoder[ContainerId] = deriveEncoder[ContainerId]

  implicit val containerIdItemsEncoder: Encoder[Items[ContainerId]] =
    deriveEncoder[Items[ContainerId]]

  implicit val containerCreateEncoder: Encoder[ContainerCreateDefinition] =
    deriveEncoder[ContainerCreateDefinition]

  implicit val containerCreateItemsEncoder: Encoder[Items[ContainerCreateDefinition]] =
    deriveEncoder[Items[ContainerCreateDefinition]]

  implicit val containerReferenceItemsEncoder: Encoder[Items[ContainerReference]] =
    deriveEncoder[Items[ContainerReference]]

  implicit val containerReadEncoder: Encoder[ContainerDefinition] =
    deriveEncoder[ContainerDefinition]

  implicit val containerReadItemsEncoder: Encoder[Items[ContainerDefinition]] =
    deriveEncoder[Items[ContainerDefinition]]

  implicit val containerCreateDecoder: Decoder[ContainerCreateDefinition] =
    deriveDecoder[ContainerCreateDefinition]

  implicit val containerCreateItemsDecoder: Decoder[Items[ContainerCreateDefinition]] =
    deriveDecoder[Items[ContainerCreateDefinition]]

  implicit val containerReferenceItemsDecoder: Decoder[Items[ContainerReference]] =
    deriveDecoder[Items[ContainerReference]]

  implicit val containerReadDecoder: Decoder[ContainerDefinition] =
    deriveDecoder[ContainerDefinition]

  implicit val containerReadItemsDecoder: Decoder[Items[ContainerDefinition]] =
    deriveDecoder[Items[ContainerDefinition]]

  implicit val containerIdDecoder: Decoder[ContainerId] = deriveDecoder[ContainerId]

  implicit val containerIdItemsDecoder: Decoder[Items[ContainerId]] =
    deriveDecoder[Items[ContainerId]]
}
