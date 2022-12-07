package com.cognite.sdk.scala.v1.containers

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.deriveDecoder

final case class ContainerReference(space: String, externalId: String) {
  val `type` = "container"
}
object ContainerReference {
  implicit val containerReferenceEncoder: Encoder[ContainerReference] =
    Encoder.forProduct3("type", "space", "externalId")((c: ContainerReference) =>
      (c.`type`, c.space, c.externalId)
    )

  implicit val containerReferenceDecoder: Decoder[ContainerReference] =
    deriveDecoder[ContainerReference]
}
