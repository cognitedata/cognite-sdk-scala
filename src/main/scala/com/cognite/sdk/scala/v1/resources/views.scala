// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1.resources

import com.cognite.sdk.scala.common._
import com.cognite.sdk.scala.v1._
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe._
import sttp.client3._
import sttp.client3.circe._

class Views[F[_]](val requestSession: RequestSession[F])
    extends WithRequestSession[F]
    with BaseUrl {
  import Views._
  override val baseUrl = uri"${requestSession.baseUrl}/models/views"

  def createItems(items: Seq[ViewCreateDefinition]): F[ViewDefinition] = {
    implicit val printer: Printer = Printer.noSpaces.copy(dropNullValues = true)
    // TODO use ItemsWithCursor[ViewDefinition] rather than  WrongItems[ViewDefinition] when the real API is used.
    requestSession.post[ViewDefinition, WrongItems[ViewDefinition], Items[ViewCreateDefinition]](
      Items(items),
      uri"$baseUrl",
      value => value.items
    )
  }

  def deleteItems(externalIds: Seq[DataModelReference]): F[Unit] =
    requestSession.post[Unit, Unit, Items[DataModelReference]](
      Items(externalIds),
      uri"$baseUrl/delete",
      _ => ()
    )
}

// TODO remove this when we work with the real backend, mock server returns a wrong response in create view case.
case class WrongItems[A](
    items: A
)

object Views {
  implicit val viewPropertyDefinition: Decoder[ViewPropertyDefinition] =
    deriveDecoder[ViewPropertyDefinition]
  implicit val viewDefinitionDecoder: Decoder[ViewDefinition] =
    deriveDecoder[ViewDefinition]
  implicit val wrongItemsDecoder: Decoder[WrongItems[ViewDefinition]] =
    deriveDecoder[WrongItems[ViewDefinition]]
// TODO remove WrongItems decoder when working with the real API and use ItemsWithCursor
//  implicit val viewDefinitionItemsWithCursorDecoder: Decoder[ItemsWithCursor[ViewDefinition]] =
//    deriveDecoder[ItemsWithCursor[ViewDefinition]]

  implicit val viewCreateDefinitionEncoder: Encoder[ViewCreateDefinition] =
    deriveEncoder[ViewCreateDefinition]
  implicit val viewCreateDefinitionItemsEncoder: Encoder[Items[ViewCreateDefinition]] =
    deriveEncoder[Items[ViewCreateDefinition]]

  implicit val dataModelReferenceItemsEncoder: Encoder[Items[DataModelReference]] =
    deriveEncoder[Items[DataModelReference]]
}
