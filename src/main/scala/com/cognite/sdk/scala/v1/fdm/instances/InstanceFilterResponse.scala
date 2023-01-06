package com.cognite.sdk.scala.v1.fdm.instances

import io.circe.generic.semiauto.deriveEncoder
import io.circe.{Decoder, Encoder, HCursor}
import com.cognite.sdk.scala.v1.resources.fdm.instances.Instances.{
  instancePropertyDefinitionDecoder,
  instancePropertyDefinitionEncoder
}

final case class InstanceFilterResponse(
    items: Seq[InstanceDefinition],
    typing: Option[Map[String, Map[String, Map[String, InstancePropertyDefinition]]]],
    nextCursor: Option[String]
)

object InstanceFilterResponse {
  implicit val instanceRetrieveResponseEncoder: Encoder[InstanceFilterResponse] = deriveEncoder

  implicit val instanceFilterResponseDecoder: Decoder[InstanceFilterResponse] = (c: HCursor) =>
    for {
      typing <- c
        .downField("typing")
        .as[Option[Map[String, Map[String, Map[String, InstancePropertyDefinition]]]]]
      nextCursor <- c.downField("nextCursor").as[Option[String]]
      items <- c
        .downField("items")
        .as[Seq[InstanceDefinition]](
          Decoder.decodeIterable[InstanceDefinition, Seq](
            InstanceDefinition.instancePropertyDefinitionBasedInstanceDecoder(
              typing
            ),
            implicitly
          )
        )
    } yield InstanceFilterResponse(items, typing, nextCursor)

}
