package com.cognite.sdk.scala.v1_0

import com.cognite.sdk.scala.common.{CogniteId, Extractor, ExtractorInstances, Resource}
import com.softwaremill.sttp.Id
import io.circe.Encoder
import io.circe.generic.semiauto._

trait ResourceV1[F[_]] extends Resource[F, CogniteId] {
  def toId(id: Long): CogniteId = CogniteId(id)
  implicit val extractor: Extractor[Id] = ExtractorInstances.idExtractor
  implicit val idEncoder: Encoder[CogniteId] = deriveEncoder
}
