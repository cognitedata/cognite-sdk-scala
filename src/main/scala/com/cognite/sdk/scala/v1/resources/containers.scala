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

  def createItems(containers: Seq[Container]): F[Seq[Container]] =
    requestSession.post[Seq[Container], Items[Container], Items[Container]](
      Items(items = containers),
      uri"$baseUrl",
      value => value.items
    )

  def list(): F[Seq[ContainerRead]] =
    requestSession.get[Seq[ContainerRead], Items[ContainerRead]](
      uri"$baseUrl",
      value => value.items
    )

  def retrieveByExternalIds(containersRefs: Seq[ContainerReference]): F[Seq[Container]] =
    requestSession.post[Seq[Container], Items[Container], Items[ContainerReference]](
      Items(items = containersRefs),
      uri"$baseUrl/byids",
      value => value.items
    )

  def delete(containersRefs: Seq[ContainerReference]): F[Unit] =
    requestSession.post[Unit, Items[Container], Items[ContainerReference]](
      Items(items = containersRefs),
      uri"$baseUrl/delete",
      _ => ()
    )
}

object Containers {
  implicit val containerReferenceEncoder: Encoder[ContainerReference] =
    Encoder.forProduct3("type", "space", "externalId")((c: ContainerReference) =>
      (c.`type`, c.space, c.externalId)
    )

  implicit val containerReferenceDecoder: Decoder[ContainerReference] =
    deriveDecoder[ContainerReference]

  implicit val indexPropertyReferenceEncoder: Encoder[IndexPropertyReference] =
    deriveEncoder[IndexPropertyReference]

  implicit val indexPropertyReferenceDecoder: Decoder[IndexPropertyReference] =
    deriveDecoder[IndexPropertyReference]

  implicit val indexDefinitionEncoder: Encoder[IndexDefinition] = deriveEncoder[IndexDefinition]

  implicit val indexDefinitionDecoder: Decoder[IndexDefinition] = deriveDecoder[IndexDefinition]

  implicit val constraintPropertyEncoder: Encoder[ConstraintProperty] =
    deriveEncoder[ConstraintProperty]

  implicit val constraintPropertyDecoder: Decoder[ConstraintProperty] =
    deriveDecoder[ConstraintProperty]

  implicit val containerPropertyDefinitionEncoder: Encoder[ContainerPropertyDefinition] =
    deriveEncoder[ContainerPropertyDefinition]

  implicit val containerPropertyDefinitionDecoder: Decoder[ContainerPropertyDefinition] =
    deriveDecoder[ContainerPropertyDefinition]

  implicit val constraintDefinitionEncoder: Encoder[ConstraintDefinition] =
    deriveEncoder[ConstraintDefinition]

  implicit val constraintDefinitionDecoder: Decoder[ConstraintDefinition] =
    deriveDecoder[ConstraintDefinition]

  implicit val containerCreateEncoder: Encoder[Container] = deriveEncoder[Container]

  implicit val containerCreateDecoder: Decoder[Container] = deriveDecoder[Container]

  implicit val containerCreateItemsEncoder: Encoder[Items[Container]] =
    deriveEncoder[Items[Container]]

  implicit val containerCreateItemsDecoder: Decoder[Items[Container]] =
    deriveDecoder[Items[Container]]

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
