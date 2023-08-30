package com.cognite.sdk.scala.v1.fdm.containers

import cats.implicits.catsSyntaxEq
import io.circe.{Decoder, DecodingFailure, Encoder, HCursor}
import io.circe.generic.semiauto.deriveDecoder
import io.circe.syntax.EncoderOps

sealed trait IndexDefinition

object IndexDefinition {
  final case class BTreeIndexDefinition(properties: Seq[String]) extends IndexDefinition {
    val indexType: String = BTreeIndexDefinition.IndexType
  }
  object BTreeIndexDefinition {
    val IndexType = "btree"
  }

  implicit val bTreeIndexDefinitionEncoder: Encoder[BTreeIndexDefinition] =
    Encoder.forProduct2("indexType", "properties")((i: BTreeIndexDefinition) =>
      (i.indexType, i.properties)
    )

  implicit val bTreeIndexDefinitionDecoder: Decoder[BTreeIndexDefinition] =
    deriveDecoder[BTreeIndexDefinition]

  implicit val indexDefinitionEncoder: Encoder[IndexDefinition] = Encoder.instance {
    case b: BTreeIndexDefinition => b.asJson
  }

  implicit val indexDefinitionDecoder: Decoder[IndexDefinition] = Decoder.instance { (c: HCursor) =>
    c.downField("indexType").as[String] match {
      case Left(err) => Left[DecodingFailure, IndexDefinition](err)
      case Right(indexType) if indexType === BTreeIndexDefinition.IndexType =>
        for {
          properties <- c.downField("properties").as[Seq[String]]
        } yield BTreeIndexDefinition(properties)
      case Right(indexType) =>
        Left[DecodingFailure, IndexDefinition](
          DecodingFailure(s"Unknown index definition: '$indexType", c.history)
        )
    }
  }
}
