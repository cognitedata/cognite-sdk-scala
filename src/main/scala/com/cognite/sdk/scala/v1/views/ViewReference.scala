// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1.views

import com.cognite.sdk.scala.v1.containers.ContainerReference
import com.cognite.sdk.scala.v1.containers.ContainerReference.containerReferenceEncoder
import io.circe.generic.semiauto.deriveDecoder
import io.circe.syntax.EncoderOps
import io.circe.{Decoder, DecodingFailure, Encoder, HCursor}

// TODO: Make it sealed and move together with ContainerReference
trait SourceReference {
  val `type`: String
}

object SourceReference {
  implicit val sourceReferenceEncoder: Encoder[SourceReference] = Encoder.instance {
    case c: ContainerReference => c.asJson
    case v: ViewReference => v.asJson
  }
  implicit val sourceReferenceDecoder: Decoder[SourceReference] =
    Decoder.instance[SourceReference] { (c: HCursor) =>
      c.downField("type").as[String] match {
        case Left(err) => Left[DecodingFailure, SourceReference](err)
        case Right(typeValue) if typeValue == ViewReference.`type` =>
          Decoder[ViewReference].apply(c)
        case Right(typeValue) if typeValue == ContainerReference.`type` =>
          Decoder[ContainerReference].apply(c)
        case Right(typeValue) =>
          Left[DecodingFailure, SourceReference](
            DecodingFailure(s"Unknown Source Reference type: $typeValue", c.history)
          )
      }
    }
}

final case class ViewReference(
    space: String,
    externalId: String,
    version: String
) extends SourceReference {
  override val `type`: String = ViewReference.`type`
}

object ViewReference {
  val `type`: String = "view"

  implicit val viewReferenceEncoder: Encoder[ViewReference] =
    Encoder.forProduct4("type", "space", "externalId", "version")((c: ViewReference) =>
      (c.`type`, c.space, c.externalId, c.version)
    )

  implicit val viewReferenceDecoder: Decoder[ViewReference] = deriveDecoder[ViewReference]
}
