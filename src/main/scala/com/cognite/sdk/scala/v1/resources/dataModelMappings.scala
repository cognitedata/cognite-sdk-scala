// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1.resources

import com.cognite.sdk.scala.common._
import com.cognite.sdk.scala.v1._
import io.circe.generic.semiauto.deriveDecoder
import io.circe.Decoder
import sttp.client3._

class DataModelMappings[F[_]](val requestSession: RequestSession[F])
    extends WithRequestSession[F]
    with RetrieveByExternalIdsWithIgnoreUnknownIds[DataModelMapping, F]
    with BaseUrl {
  import DataModelMappings._
  override val baseUrl = uri"${requestSession.baseUrl}/datamodelstorage/mappings"

  def list(): F[Seq[DataModelMapping]] =
    requestSession.postEmptyBody[Seq[DataModelMapping], Items[DataModelMapping]](
      uri"$baseUrl/list",
      value => value.items
    )

  def retrieveByExternalIds(
      externalIds: Seq[String],
      ignoreUnknownIds: Boolean
  ): F[Seq[DataModelMapping]] =
    RetrieveByExternalIdsWithIgnoreUnknownIds.retrieveByExternalIds(
      requestSession,
      uri"$baseUrl",
      externalIds,
      ignoreUnknownIds
    )
}

object DataModelMappings {
  import DataModels.dataModelPropertyDecoder
  implicit val dataModelMappingDecoder: Decoder[DataModelMapping] = deriveDecoder[DataModelMapping]
  implicit val dataModelMappingItemsDecoder: Decoder[Items[DataModelMapping]] =
    deriveDecoder[Items[DataModelMapping]]
}
