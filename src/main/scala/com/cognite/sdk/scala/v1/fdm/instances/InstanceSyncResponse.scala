package com.cognite.sdk.scala.v1.fdm.instances

import cats.implicits.toTraverseOps
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
      items <- itemObjects
        .map {
          _.toList
            .traverse { case (groupName, values) =>
              values
                .traverse { item =>
                  item
                    .as[InstanceDefinition](
                      InstanceDefinition.instancePropertyDefinitionBasedInstanceDecoder(
                        typing.flatMap(_.get(groupName))
                      )
                    )
                }
                .map((groupName, _))
            }
            .map(_.toMap)
        }
        .traverse(identity)

    } yield InstanceSyncResponse(items, nextCursor, typing)
}
