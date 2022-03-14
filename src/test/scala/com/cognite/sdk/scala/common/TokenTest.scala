// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.common

import cats.effect.IO
import com.cognite.sdk.scala.v1.RequestSession
import org.scalatest.OptionValues
import sttp.client3._
import sttp.client3.asynchttpclient.cats.AsyncHttpClientCatsBackend

import scala.concurrent.duration._
import cats.effect.unsafe.implicits.global

class TokenTest extends SdkTestSpec with OptionValues {
  val tenant: String = sys.env("TEST_AAD_TENANT_BLUEFIELD")
  val clientId: String = sys.env("TEST_CLIENT_ID_BLUEFIELD")
  val clientSecret: String = sys.env("TEST_CLIENT_SECRET_BLUEFIELD")

  implicit val sttpBackend: SttpBackend[IO, Any] = AsyncHttpClientCatsBackend[IO]().unsafeRunSync()

  it should "read token inspect result" in {

    val credentials = OAuth2.ClientCredentials(
      tokenUri = uri"https://login.microsoftonline.com/$tenant/oauth2/v2.0/token",
      clientId = clientId,
      clientSecret = clientSecret,
      scopes = List("https://bluefield.cognitedata.com/.default"),
      cdfProjectName = "extractor-bluefield-testing"
    )

    val authProvider = OAuth2.ClientCredentialsProvider[IO](credentials)
      .unsafeRunTimed(1.second)
      .value

    val token =
      new Token(RequestSession[IO]("CogniteScalaSDK-OAuth-Test", uri"https://bluefield.cognitedata.com", sttpBackend, authProvider))
    val status = token.inspect().unsafeRunTimed(10.seconds).value
    assert(status.subject !== "")
  }
}
