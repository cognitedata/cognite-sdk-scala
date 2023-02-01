package com.cognite.sdk.scala.playground

import cats.Monad
import com.cognite.sdk.scala.common.AuthProvider
import com.cognite.sdk.scala.playground.resources.WellDataLayer
import com.cognite.sdk.scala.v1.GenericClient.parseBaseUrlOrThrow
import com.cognite.sdk.scala.v1.RequestSession
import sttp.client3.{SttpBackend, UriContext}
import sttp.model.Uri

class PlaygroundClient[F[_]: Monad](
    applicationName: String,
    val projectName: String,
    baseUrl: String,
    authProvider: AuthProvider[F],
    clientTag: Option[String],
    cdfVersion: Option[String]
)(implicit sttpBackend: SttpBackend[F, Any]) {
  val uri: Uri = parseBaseUrlOrThrow(baseUrl)
  lazy val wellDataLayerRequestSession: RequestSession[F] =
    RequestSession(
      applicationName,
      uri"$uri/api/playground/projects/$projectName",
      sttpBackend,
      authProvider,
      clientTag,
      cdfVersion
    )
  lazy val wdl = new WellDataLayer[F](wellDataLayerRequestSession)
}
