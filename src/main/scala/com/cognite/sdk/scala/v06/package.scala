package com.cognite.sdk.scala

import com.cognite.sdk.scala.common.{Extractor, ExtractorInstances}
import com.softwaremill.sttp.{HttpURLConnectionBackend, Id, SttpBackend}

package object v06 {
  implicit val sttpBackend: SttpBackend[Id, Nothing] = HttpURLConnectionBackend()
  implicit val extractor: Extractor[Data] = ExtractorInstances.dataExtractor
}
