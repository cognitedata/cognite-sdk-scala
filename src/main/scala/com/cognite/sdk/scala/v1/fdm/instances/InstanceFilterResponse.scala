package com.cognite.sdk.scala.v1.fdm.instances

import com.cognite.sdk.scala.common.DebugNotice
import io.circe.generic.semiauto.deriveDecoder
import io.circe.{Decoder, HCursor}

final case class InstanceFilterResponse(
    items: Seq[InstanceDefinition],
    typing: Option[Map[String, Map[String, Map[String, TypePropertyDefinition]]]],
    nextCursor: Option[String],
    debug: Option[DebugNotices] = None
)

final case class DebugNotices(notices: Seq[DebugNotice])

object InstanceFilterResponse {

  import com.cognite.sdk.scala.common.DebugNotice._
  implicit val debugNoticesDecoder: Decoder[DebugNotices] = deriveDecoder

  implicit val instanceFilterResponseDecoder: Decoder[InstanceFilterResponse] = (c: HCursor) =>
    for {
      typing <- c
        .downField("typing")
        .as[Option[Map[String, Map[String, Map[String, TypePropertyDefinition]]]]]
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
      debug <- c
        .downField("debug")
        .as[Option[DebugNotices]]
    } yield InstanceFilterResponse(items, typing, nextCursor, debug)

}
