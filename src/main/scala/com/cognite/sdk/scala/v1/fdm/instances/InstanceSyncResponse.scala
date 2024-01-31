package com.cognite.sdk.scala.v1.fdm.instances

import io.circe.generic.semiauto.deriveEncoder
import io.circe.{Decoder, Encoder, HCursor}

final case class InstanceSyncResponse(
    items: Option[Map[String, Seq[InstanceDefinition]]] = None,
    nextCursor: Option[Map[String, String]] = None
)

object InstanceSyncResponse {
  implicit val instanceSyncResponseEncoder: Encoder[InstanceSyncResponse] = deriveEncoder

  implicit val instanceSyncResponseDecoder: Decoder[InstanceSyncResponse] = (c: HCursor) =>
    for {
      nextCursor <- c.downField("nextCursor").as[Option[Map[String, String]]]
      items <- c
        .downField("items")
        .as[Option[Map[String, Seq[InstanceDefinition]]]](
          Decoder.decodeOption[Map[String, Seq[InstanceDefinition]]](
            Decoder.decodeMap[String, Seq[InstanceDefinition]](
              implicitly,
              Decoder.decodeIterable[InstanceDefinition, Seq](
                InstanceDefinition.instancePropertyDefinitionBasedInstanceDecoder(
                  None
                ),
                implicitly
              )
            )
          )
        )

    } yield InstanceSyncResponse(items, nextCursor)
}
