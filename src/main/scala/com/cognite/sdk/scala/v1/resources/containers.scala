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
  override val baseUrl = uri"${requestSession.baseUrl}/models/containers"
  import Containers._

  def createItems(containers: Seq[ContainerCreate]): F[Seq[ContainerCreate]] = {
    implicit val nullDroppingPrinter: Printer = Printer.noSpaces.copy(dropNullValues = true)
    val x =
      requestSession.post[Seq[ContainerCreate], Items[ContainerCreate], Items[ContainerCreate]](
        Items(items = containers),
        uri"$baseUrl",
        value => value.items
      )
    x
  }

}

object Containers {
  implicit val containerReferenceEncoder: Encoder[ContainerReference] =
    deriveEncoder[ContainerReference]

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

  implicit val containerCreateEncoder: Encoder[ContainerCreate] = deriveEncoder[ContainerCreate]

  implicit val containerCreateDecoder: Decoder[ContainerCreate] = deriveDecoder[ContainerCreate]

  implicit val containerCreateItemsEncoder: Encoder[Items[ContainerCreate]] =
    deriveEncoder[Items[ContainerCreate]]

  implicit val containerCreateItemsDecoder: Decoder[Items[ContainerCreate]] =
    deriveDecoder[Items[ContainerCreate]]
}
