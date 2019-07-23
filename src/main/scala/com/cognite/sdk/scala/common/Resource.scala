package com.cognite.sdk.scala.common

import com.cognite.sdk.scala.v1.RequestSession
import com.softwaremill.sttp.Uri

object Resource {
  val defaultLimit: Long = 1000
}

trait BaseUri {
  val baseUri: Uri
}

trait WithRequestSession[F[_]] {
  val requestSession: RequestSession[F]
}
