// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1.resources

import com.cognite.sdk.scala.common._
import com.cognite.sdk.scala.v1._
import io.circe.{Decoder, Encoder, Printer}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import sttp.client3._
import sttp.client3.circe._

class DataModelInstances[F[_]](val requestSession: RequestSession[F])
    extends WithRequestSession[F]
    with BaseUrl {

  import DataModelInstances._

  override val baseUrl = uri"${requestSession.baseUrl}/datamodelstorage/instances"

  def createItems(items: Items[DataModelInstance]): F[Seq[DataModelInstance]] = {
    implicit val printer: Printer = Printer.noSpaces.copy(dropNullValues = true)
    requestSession.post[Seq[DataModelInstance], Items[DataModelInstance], Items[
      DataModelInstance
    ]](
      items,
      uri"$baseUrl/ingest",
      value => value.items
    )
  }
}

object DataModelInstances {
  implicit val dataModelInstanceEncoder: Encoder[DataModelInstance] =
    deriveEncoder[DataModelInstance]
  implicit val dataModelInstanceItemsEncoder: Encoder[Items[DataModelInstance]] =
    deriveEncoder[Items[DataModelInstance]]
  implicit val dataModelInstanceDecoder: Decoder[DataModelInstance] =
    deriveDecoder[DataModelInstance]
  implicit val dataModelInstanceItemsDecoder: Decoder[Items[DataModelInstance]] =
    deriveDecoder[Items[DataModelInstance]]
}
