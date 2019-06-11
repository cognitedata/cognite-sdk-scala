package com.cognite.sdk.scala.common

import com.softwaremill.sttp.{Empty, RequestT, Uri, sttp}
import scala.concurrent.duration._

abstract class Resource[F[_], InternalId, PrimitiveId](auth: Auth) {
  val request: RequestT[Empty, String, Nothing] = sttp
    .auth(auth)
    .contentType("application/json")
    .header("accept", "application/json")
    .readTimeout(90.seconds)
    .parseResponseIf(_ => true)
  val baseUri: Uri
  val defaultLimit: Long = 1000
  def toInternalId(id: PrimitiveId): InternalId
}
