package com.cognite.sdk.scala.common

import com.softwaremill.sttp.{Empty, RequestT, Uri, sttp}
import scala.concurrent.duration._

abstract class Resource[F[_], InternalId, PrimitiveId](auth: Auth)
    extends BaseUri
    with RequestSession
    with ToInternalId[InternalId, PrimitiveId] {}

object Resource {
  val defaultLimit: Long = 1000
}

trait BaseUri {
  val baseUri: Uri
}

trait RequestSession {
  def request(implicit auth: Auth): RequestT[Empty, String, Nothing] =
    sttp
      .auth(auth)
      .contentType("application/json")
      .header("accept", "application/json")
      .readTimeout(90.seconds)
      .parseResponseIf(_ => true)
}

trait ToInternalId[InternalId, PrimitiveId] {
  def toInternalId(id: PrimitiveId): InternalId
}
