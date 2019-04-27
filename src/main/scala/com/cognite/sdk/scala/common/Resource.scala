package com.cognite.sdk.scala.common

import com.softwaremill.sttp.Uri

abstract class Resource {
  val baseUri: Uri
  val defaultLimit: Int = 1000
}
