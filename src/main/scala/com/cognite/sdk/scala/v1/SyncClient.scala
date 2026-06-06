// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1

import cats.implicits._
import cats.Id
import com.cognite.sdk.scala.common._
import com.cognite.sdk.scala.sttp.GzipBackend
import natchez.Trace
import sttp.client3._

class SyncClient(
    applicationName: String,
    override val projectName: String,
    baseUrl: String =
      Option(System.getenv("COGNITE_BASE_URL")).getOrElse("https://api.cognitedata.com"),
    auth: Auth
)(implicit trace: Trace[OrError], sttpBackend: SttpBackend[OrError, Any])
    extends GenericClient[OrError](
      applicationName,
      projectName,
      baseUrl,
      auth,
      sttpBackend = sttpBackend
    )

object SyncClient {
  implicit val sttpBackend: SttpBackend[OrError, Any] =
    new EitherBackend(new GzipBackend[Id, Any](HttpURLConnectionBackend()))

  def apply(
      applicationName: String,
      projectName: String,
      baseUrl: String,
      auth: Auth
  )(
      implicit trace: Trace[OrError],
      sttpBackend: SttpBackend[OrError, Any]
  ): SyncClient = new SyncClient(applicationName, projectName, baseUrl, auth)
}
