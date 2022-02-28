// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1.resources

import com.cognite.sdk.scala.common._
import com.cognite.sdk.scala.v1._
import io.circe.Encoder
import io.circe.generic.semiauto.deriveEncoder
import sttp.client3._
import sttp.client3.circe._

class DataModelMappings[F[_]](val requestSession: RequestSession[F])
    extends WithRequestSession[F]
    with BaseUrl {
  import DataModelMappings._
  import DataModels.dataModelItemsDecoder
  override val baseUrl = uri"${requestSession.baseUrl}/datamodelstorage/definitions"

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

object DataModelMappings {

  implicit val dataModelGetByCogniteIdIdsEncoder
      : Encoder[DataModelGetByExternalIdsInput[CogniteId]] =
    deriveEncoder[DataModelGetByExternalIdsInput[CogniteId]]

  implicit val dataModelGetByExternalIdsEncoder
      : Encoder[DataModelGetByExternalIdsInput[CogniteExternalId]] =
    deriveEncoder[DataModelGetByExternalIdsInput[CogniteExternalId]]

}
