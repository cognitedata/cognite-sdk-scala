package com.cognite.sdk.scala.common

import com.softwaremill.sttp.{Empty, RequestT, Uri, sttp}
import scala.concurrent.duration._

abstract class Resource[F[_], InternalId, PrimitiveId](auth: Auth)
  extends BaseUri
  with RequestSession
  with ToInternalId[InternalId, PrimitiveId] {
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

trait BaseUri {
  val baseUri: Uri
}

trait RequestSession {
  val request: RequestT[Empty, String, Nothing]
}

trait ToInternalId[InternalId, PrimitiveId] {
  def toInternalId(id: PrimitiveId): InternalId
}
