package com.cognite.sdk.scala.v1_0

import com.cognite.sdk.scala.common.{Extractor, Resource}
import com.softwaremill.sttp.Id

abstract class ResourceV1[F[_]] extends Resource[F] {
  implicit val extractor: Extractor[Id] = ExtractorInstances.idExtractor
}
