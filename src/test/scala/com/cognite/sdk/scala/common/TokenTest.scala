// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.common

import cats.effect.{ContextShift, IO, Timer}
import com.cognite.sdk.scala.v1.RequestSession
import com.softwaremill.sttp._
import com.softwaremill.sttp.asynchttpclient.cats.AsyncHttpClientCatsBackend

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

class TokenTest extends SdkTestSpec {
  val tenant: String = sys.env("TEST_AAD_TENANT_BLUEFIELD")
  val clientId: String = sys.env("TEST_CLIENT_ID_BLUEFIELD")
  val clientSecret: String = sys.env("TEST_CLIENT_SECRET_BLUEFIELD")

  implicit val cs: ContextShift[IO] = IO.contextShift(global)
  implicit val timer: Timer[IO] = IO.timer(global)

  implicit val sttpBackend: SttpBackend[IO, Nothing] = AsyncHttpClientCatsBackend[IO]()

  it should "read token inspect result" in {

    val credentials = OAuth2.ClientCredentials(
      tokenUri = uri"https://login.microsoftonline.com/$tenant/oauth2/v2.0/token",
      clientId = clientId,
      clientSecret = clientSecret,
      scopes = List("https://bluefield.cognitedata.com/.default")
    )

    val authProvider = OAuth2.ClientCredentialsProvider[IO](credentials).unsafeRunTimed(1.second).get

    val token =
      new Token(RequestSession[IO]("CogniteScalaSDK-OAuth-Test", uri"https://bluefield.cognitedata.com", sttpBackend, authProvider))
    val status = token.inspect().unsafeRunTimed(10.seconds).get
    assert(status.subject != "")
  }
}
