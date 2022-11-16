// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1.resources

import com.cognite.sdk.scala.common._
import com.cognite.sdk.scala.v1._
import io.circe.generic.semiauto.{deriveCodec, deriveDecoder, deriveEncoder}
import io.circe._
import sttp.client3._
import sttp.client3.circe._

class Views[F[_]](val requestSession: RequestSession[F])
    extends WithRequestSession[F]
    with BaseUrl {
  import Views._
  override val baseUrl = uri"${requestSession.baseUrl}/models/views"

  def createItems(items: Seq[ViewCreateDefinition]): F[Seq[ViewDefinition]] = {
    implicit val printer: Printer = Printer.noSpaces.copy(dropNullValues = true)
    requestSession.post[Seq[ViewDefinition], ItemsWithCursor[ViewDefinition], Items[ViewCreateDefinition]](
      Items(items),
      uri"$baseUrl",
      value => value.items
    )
  }
}

object Views {
  implicit val viewCreateDefinitionCodec: Encoder[ViewCreateDefinition] =
    deriveEncoder[ViewCreateDefinition]
  implicit val viewCreateDefinitionItemsCodec: Encoder[Items[ViewCreateDefinition]] =
    deriveEncoder[Items[ViewCreateDefinition]]
  implicit val viewDefinitionCodec: Decoder[ViewDefinition] =
    deriveDecoder[ViewDefinition]
  implicit val viewDefinitionItemsCodec: Decoder[Items[ViewDefinition]] =
    deriveDecoder[Items[ViewDefinition]]
  implicit val viewDefinitionItemsWithCursorCodec: Decoder[ItemsWithCursor[ViewDefinition]] =
    deriveDecoder[ItemsWithCursor[ViewDefinition]]
}
