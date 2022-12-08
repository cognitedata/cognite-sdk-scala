package com.cognite.sdk.scala.v1.fdm

import com.cognite.sdk.scala.v1.fdm.containers.ContainerReference
import com.cognite.sdk.scala.v1.fdm.views.ViewReference
import io.circe.syntax.EncoderOps
import io.circe.{Decoder, DecodingFailure, Encoder, HCursor}

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
