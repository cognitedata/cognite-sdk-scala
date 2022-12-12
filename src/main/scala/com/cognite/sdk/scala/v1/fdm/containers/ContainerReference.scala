package com.cognite.sdk.scala.v1.fdm.containers

import com.cognite.sdk.scala.v1.fdm.common.refs.{SourceReference, SourceType}
import io.circe.generic.semiauto.deriveDecoder
import io.circe.{Decoder, Encoder}

final case class ContainerReference(space: String, externalId: String) extends SourceReference {
  val `type`: SourceType = SourceType.Container
}

object ContainerReference {
  implicit val containerReferenceEncoder: Encoder[ContainerReference] =
    Encoder.forProduct3("type", "space", "externalId")((c: ContainerReference) =>
      (c.`type`, c.space, c.externalId)
    )

  implicit val containerReferenceDecoder: Decoder[ContainerReference] =
    deriveDecoder[ContainerReference]
}
