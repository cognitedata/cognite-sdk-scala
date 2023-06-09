package com.cognite.sdk.scala.v1.fdm.common

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

final case class DataModelReference(
    space: String,
    externalId: String,
    version: Option[String]
)

object DataModelReference {
  implicit val dataModelReferenceEncoder: Encoder[DataModelReference] =
    deriveEncoder[DataModelReference]
  implicit val dataModelReferenceDecoder: Decoder[DataModelReference] =
    deriveDecoder[DataModelReference]
}
