package com.cognite.sdk.scala.v1.fdm.datamodels

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

final case class DataModelCreate(
    space: String,
    externalId: String,
    name: Option[String],
    description: Option[String],
    version: String,
    views: Option[Seq[DataModelCreateViewReference]]
)

object DataModelCreate {
  implicit val dataModelCreateEncoder: Encoder[DataModelCreate] =
    deriveEncoder[DataModelCreate]
  implicit val dataModelCreateDecoder: Decoder[DataModelCreate] =
    deriveDecoder[DataModelCreate]
}
