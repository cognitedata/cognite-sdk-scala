package com.cognite.sdk.scala.v1

import com.cognite.sdk.scala.common.{CogniteId, Extractor, ExtractorInstances}
import com.softwaremill.sttp.Id
import io.circe.Encoder
import io.circe.generic.semiauto._

trait ResourceV1[F[_]] {
  def toInternalId(id: Long): CogniteId = CogniteId(id)
  implicit val extractor: Extractor[Id] = ExtractorInstances.idExtractor
  implicit val idEncoder: Encoder[CogniteId] = deriveEncoder
}
