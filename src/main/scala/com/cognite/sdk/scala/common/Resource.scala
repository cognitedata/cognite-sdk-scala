package com.cognite.sdk.scala.common

import com.softwaremill.sttp.{Empty, RequestT, SttpBackend, Uri, sttp}
import scala.concurrent.duration._

abstract class Resource[F[_], I] {
  implicit val auth: Auth
  implicit val sttpBackend: SttpBackend[F, _]

  val request: RequestT[Empty, String, Nothing] = sttp
    .auth(auth)
    .contentType("application/json")
    .readTimeout(90.seconds)
    .parseResponseIf(_ => true)
  val baseUri: Uri
  val defaultLimit: Long = 1000
  def toId(id: Long): I
}
