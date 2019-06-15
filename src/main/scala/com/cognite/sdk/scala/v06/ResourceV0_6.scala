package com.cognite.sdk.scala.v06

import com.cognite.sdk.scala.common.{Extractor, ExtractorInstances, Resource}
import io.circe.Encoder

trait ResourceV0_6[F[_]] extends Resource[F, Long, Long] {
  def toInternalId(id: Long): Long = id
  implicit val extractor: Extractor[Data] = ExtractorInstances.dataExtractor
  implicit val idEncoder: Encoder[Long] = Encoder.encodeLong
}
