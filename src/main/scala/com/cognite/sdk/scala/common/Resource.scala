package com.cognite.sdk.scala.common

import com.softwaremill.sttp.{Empty, RequestT, SttpBackend, Uri, sttp}

abstract class Resource[F[_]] {
  implicit val auth: Auth
  implicit val sttpBackend: SttpBackend[F, _]

  val request: RequestT[Empty, String, Nothing] = sttp
    .auth(auth)
    .contentType("application/json")
  val baseUri: Uri
  val defaultLimit: Int = 1000
}
