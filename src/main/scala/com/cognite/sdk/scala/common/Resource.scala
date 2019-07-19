package com.cognite.sdk.scala.common

import com.softwaremill.sttp.{Empty, RequestT, Uri}

object Resource {
  val defaultLimit: Long = 1000
}

trait BaseUri {
  val baseUri: Uri
}

trait RequestSession {
  def request: RequestT[Empty, String, Nothing]
  val baseUri: Uri
}

trait WithRequestSession {
  val requestSession: RequestSession
}
