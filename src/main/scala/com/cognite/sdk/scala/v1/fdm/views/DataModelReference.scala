package com.cognite.sdk.scala.v1.fdm.views

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class DataModelReference(
    space: String,
    externalId: String,
    version: String
)

object DataModelReference {
  implicit val dataModelReferenceEncoder: Encoder[DataModelReference] =
    deriveEncoder[DataModelReference]
  implicit val dataModelReferenceDecoder: Decoder[DataModelReference] =
    deriveDecoder[DataModelReference]
}
