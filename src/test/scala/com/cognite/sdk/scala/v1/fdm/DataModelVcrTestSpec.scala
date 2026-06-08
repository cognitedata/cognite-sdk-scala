package com.cognite.sdk.scala.v1.fdm

import com.cognite.sdk.scala.common.VcrTestSpec

abstract class DataModelVcrTestSpec extends VcrTestSpec {
  override protected def envVarSuffix: String = ""
  override def projectName: String = sys.env.getOrElse("TEST_PROJECT", "extractor-bluefield-testing")
  override def baseUrl: String = sys.env.getOrElse("COGNITE_BASE_URL", "https://bluefield.cognitedata.com")
  override protected def cdfVersion: Option[String] = Some("alpha")
}
