// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1.resources

import com.cognite.sdk.scala.common._
import com.cognite.sdk.scala.v1._
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder, Printer}
import sttp.client3._
import sttp.client3.circe._

class DataModels[F[_]](val requestSession: RequestSession[F])
    extends WithRequestSession[F]
    with BaseUrl {
  import DataModels._
  override val baseUrl = uri"${requestSession.baseUrl}/datamodelstorage"

  def createItems(items: Items[DataModel]): F[Seq[DataModel]] = {
    implicit val printer: Printer = Printer.noSpaces.copy(dropNullValues = true)
    requestSession.post[Seq[DataModel], Items[DataModel], Items[DataModel]](
      items,
      uri"$baseUrl/definitions/apply",
      value => value.items
    )
  }

  def deleteItems(externalIds: Seq[String], ignoreUnknownIds: Boolean): F[Unit] =
    requestSession.post[Unit, Unit, ItemsWithIgnoreUnknownIds[CogniteId]](
      ItemsWithIgnoreUnknownIds(externalIds.map(CogniteExternalId(_)), ignoreUnknownIds),
      uri"$baseUrl/definitions/delete",
      _ => ()
    )

  def list(): F[Seq[DataModel]] =
    requestSession.postEmptyBody[Seq[DataModel], Items[DataModel]](
      uri"$baseUrl/definitions/list",
      value => value.items
    )

}

object DataModels {
  implicit val dataModelPropertyIndexEncoder: Encoder[DataModelPropertyIndex] =
    deriveEncoder[DataModelPropertyIndex]
  implicit val dataModelPropertyEncoder: Encoder[DataModelProperty] =
    deriveEncoder[DataModelProperty]
  implicit val dataModelEncoder: Encoder[DataModel] = deriveEncoder[DataModel]
  implicit val dataModelItemsEncoder: Encoder[Items[DataModel]] = deriveEncoder[Items[DataModel]]

  implicit val dataModelPropertyIndexDecoder: Decoder[DataModelPropertyIndex] =
    deriveDecoder[DataModelPropertyIndex]
  implicit val dataModelPropertyDecoder: Decoder[DataModelProperty] =
    deriveDecoder[DataModelProperty]
  implicit val dataModelDecoder: Decoder[DataModel] = deriveDecoder[DataModel]
  implicit val dataModelItemsDecoder: Decoder[Items[DataModel]] = deriveDecoder[Items[DataModel]]

}
