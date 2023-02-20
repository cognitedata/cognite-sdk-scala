package com.cognite.sdk.scala.v1.fdm.common

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class DirectRelationReference(space: String, externalId: String)

object DirectRelationReference {
  implicit val directRelationReferenceEncoder: Encoder[DirectRelationReference] = deriveEncoder
  implicit val directRelationReferenceDecoder: Decoder[DirectRelationReference] = deriveDecoder
}
