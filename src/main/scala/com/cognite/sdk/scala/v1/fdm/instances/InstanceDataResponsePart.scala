package com.cognite.sdk.scala.v1.fdm.instances

import cats.implicits.toTraverseOps
import io.circe.{Decoder, HCursor, Json}

final case class InstanceDataResponsePart(
    items: Option[Map[String, Seq[InstanceDefinition]]] = None,
    typing: Option[Map[String, Map[String, Map[String, Map[String, TypePropertyDefinition]]]]] =
      None,
    debug: Option[DebugNotices]
)

object InstanceDataResponsePart {
  import com.cognite.sdk.scala.common.DebugNotice._

  val instanceDataResponsePartDecoder: Decoder[InstanceDataResponsePart] = (c: HCursor) =>
    for {
      typing <- c
        .downField("typing")
        .as[Option[Map[String, Map[String, Map[String, Map[String, TypePropertyDefinition]]]]]]
      itemObjects <- c
        .downField("items")
        .as[Option[Map[String, Seq[Json]]]]
      debug <- c
        .downField("debug")
        .as[Option[DebugNotices]]
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

    } yield InstanceDataResponsePart(items, typing, debug)
}
