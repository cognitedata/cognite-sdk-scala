package com.cognite.sdk.scala.v1.fdm.instances

import cats.implicits.toTraverseOps
import io.circe.generic.semiauto.deriveEncoder
import io.circe.{Decoder, Encoder, HCursor, Json}

final case class InstanceDataResponse(
    items: Option[Map[String, Seq[InstanceDefinition]]] = None,
    nextCursor: Option[Map[String, String]] = None,
    typing: Option[Map[String, Map[String, Map[String, Map[String, TypePropertyDefinition]]]]] =
      None
)

object InstanceDataResponse {
  implicit val instanceDataResponseEncoder: Encoder[InstanceDataResponse] = deriveEncoder

  implicit val instanceDataResponseDecoder: Decoder[InstanceDataResponse] = (c: HCursor) =>
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
              values.toList
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
        .traverse(decodeResult => decodeResult)

    } yield InstanceDataResponse(items, nextCursor, typing)
}
