package com.cognite.sdk.scala.v1.fdm.common

import com.cognite.sdk.scala.v1.fdm.containers.ContainerReference
import com.cognite.sdk.scala.v1.fdm.views.ViewReference
import io.circe.syntax.EncoderOps
import io.circe.{Decoder, DecodingFailure, Encoder, HCursor}

trait SourceReference {
  val `type`: SourceType
}

object SourceReference {
  implicit val sourceReferenceEncoder: Encoder[SourceReference] = Encoder.instance {
    case c: ContainerReference => c.asJson
    case v: ViewReference => v.asJson
  }

  implicit val sourceReferenceDecoder: Decoder[SourceReference] =
    Decoder.instance[SourceReference] { (c: HCursor) =>
      c.downField("type").as[SourceType] match {
        case Left(err) => Left[DecodingFailure, SourceReference](err)
        case Right(SourceType.View) => Decoder[ViewReference].apply(c)
        case Right(SourceType.Container) => Decoder[ContainerReference].apply(c)
      }
    }
}
