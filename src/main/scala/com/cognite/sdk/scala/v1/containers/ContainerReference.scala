package com.cognite.sdk.scala.v1.containers

import com.cognite.sdk.scala.v1.views.SourceReference
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.deriveDecoder

final case class ContainerReference(space: String, externalId: String) extends SourceReference {
  val `type`: String = ContainerReference.`type`
}

object ContainerReference {
  val `type`: String = "container"

  implicit val containerReferenceEncoder: Encoder[ContainerReference] =
    Encoder.forProduct3("type", "space", "externalId")((c: ContainerReference) =>
      (c.`type`, c.space, c.externalId)
    )

  implicit val containerReferenceDecoder: Decoder[ContainerReference] =
    deriveDecoder[ContainerReference]
}
