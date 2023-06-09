package com.cognite.sdk.scala.v1.fdm.datamodels

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

final case class DataModel(
    space: String,
    externalId: String,
    name: Option[String],
    description: Option[String],
    version: String,
    views: Option[Seq[DataModelViewReference]],
    createdTime: Long,
    lastUpdatedTime: Long
)

object DataModel {
  implicit val dataModelEncoder: Encoder[DataModel] =
    deriveEncoder[DataModel]
  implicit val dataModelDecoder: Decoder[DataModel] =
    deriveDecoder[DataModel]
}
