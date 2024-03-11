package com.cognite.sdk.scala.v1.fdm.instances

import io.circe.generic.semiauto.deriveEncoder
import io.circe.{Decoder, Encoder, HCursor, Json}

final case class InstanceSyncResponse(
    items: Option[Map[String, Seq[InstanceDefinition]]] = None,
    nextCursor: Option[Map[String, String]] = None,
    typing: Option[Map[String, Map[String, Map[String, Map[String, TypePropertyDefinition]]]]] =
      None
)

object InstanceSyncResponse {
  implicit val instanceSyncResponseEncoder: Encoder[InstanceSyncResponse] = deriveEncoder

  implicit val instanceSyncResponseDecoder: Decoder[InstanceSyncResponse] = (c: HCursor) =>
    for {
      nextCursor <- c.downField("nextCursor").as[Option[Map[String, String]]]
      typing <- c
        .downField("typing")
        .as[Option[Map[String, Map[String, Map[String, Map[String, TypePropertyDefinition]]]]]]
      itemObjects <- c
        .downField("items")
        .as[Option[Map[String, Seq[Json]]]]
      items: Option[Map[String, Seq[InstanceDefinition]]] = itemObjects.map { itemObjects =>
        itemObjects.map { case (key, value) =>
          key -> value.flatMap { item =>
            item
              .as[InstanceDefinition](
                InstanceDefinition.instancePropertyDefinitionBasedInstanceDecoder(
                  typing.flatMap(_.get(key))
                )
              )
              .toOption
          }
        }
      }

    } yield InstanceSyncResponse(items, nextCursor, typing)
}
