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
  override val baseUrl = uri"${requestSession.baseUrl}/datamodelstorage/definitions"

  def createItems(items: Items[DataModel]): F[Seq[DataModel]] = {
    implicit val printer: Printer = Printer.noSpaces.copy(dropNullValues = true)
    requestSession.post[Seq[DataModel], Items[DataModel], Items[DataModel]](
      items,
      uri"$baseUrl/apply",
      value => value.items
    )
  }

  def deleteItems(externalIds: Seq[String]): F[Unit] =
    requestSession.post[Unit, Unit, Items[CogniteId]](
      Items(externalIds.map(CogniteExternalId(_))),
      uri"$baseUrl/delete",
      _ => ()
    )

  def list(includeInheritedProperties: Boolean = false): F[Seq[DataModel]] =
    requestSession.post[Seq[DataModel], Items[DataModel], DataModelListInput](
      DataModelListInput(includeInheritedProperties),
      uri"$baseUrl/list",
      value => value.items
    )

  def retrieveByExternalIds(
      externalIds: Seq[String],
      includeInheritedProperties: Boolean = false,
      ignoreUnknownIds: Boolean = false
  ): F[Seq[DataModel]] =
    requestSession
      .post[Seq[DataModel], Items[DataModel], DataModelGetByExternalIdsInput[
        CogniteId
      ]](
        DataModelGetByExternalIdsInput(
          externalIds.map(CogniteExternalId(_)),
          includeInheritedProperties,
          ignoreUnknownIds
        ),
        uri"$baseUrl/byids",
        value => value.items
      )

}

object DataModels {
  implicit val dataModelPropertyIndexEncoder: Encoder[DataModelPropertyIndex] =
    deriveEncoder[DataModelPropertyIndex]
  implicit val dataModelPropertyEncoder: Encoder[DataModelPropertyDeffinition] =
    deriveEncoder[DataModelPropertyDeffinition]
  implicit val dataModelEncoder: Encoder[DataModel] = deriveEncoder[DataModel]
  implicit val dataModelItemsEncoder: Encoder[Items[DataModel]] = deriveEncoder[Items[DataModel]]
  implicit val dataModelListInputEncoder: Encoder[DataModelListInput] =
    deriveEncoder[DataModelListInput]

  implicit val dataModelPropertyIndexDecoder: Decoder[DataModelPropertyIndex] =
    deriveDecoder[DataModelPropertyIndex]
  implicit val dataModelPropertyDecoder: Decoder[DataModelPropertyDeffinition] =
    deriveDecoder[DataModelPropertyDeffinition]
  implicit val dataModelDecoder: Decoder[DataModel] = deriveDecoder[DataModel]
  implicit val dataModelItemsDecoder: Decoder[Items[DataModel]] = deriveDecoder[Items[DataModel]]

  implicit val dataModelGetByCogniteIdIdsEncoder
      : Encoder[DataModelGetByExternalIdsInput[CogniteId]] =
    deriveEncoder[DataModelGetByExternalIdsInput[CogniteId]]

  implicit val dataModelGetByExternalIdsEncoder
      : Encoder[DataModelGetByExternalIdsInput[CogniteExternalId]] =
    deriveEncoder[DataModelGetByExternalIdsInput[CogniteExternalId]]

}
