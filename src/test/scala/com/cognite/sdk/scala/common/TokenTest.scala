// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.common

import cats.effect.IO
import com.cognite.sdk.scala.v1.RequestSession
import org.scalatest.OptionValues
import sttp.client3._
import sttp.client3.asynchttpclient.cats.AsyncHttpClientCatsBackend

import scala.concurrent.duration._

class TokenTest extends SdkTestSpec with OptionValues {
  implicit val sttpBackend: SttpBackend[IO, Any] = AsyncHttpClientCatsBackend[IO]().unsafeRunSync()

  it should "read token inspect result" in {
    val authProvider = OAuth2.ClientCredentialsProvider[IO](credentials)
      .unsafeRunTimed(1.second)
      .value

    val token =
      new Token(RequestSession[IO]("CogniteScalaSDK-OAuth-Test", uri"${baseUrl}", sttpBackend,
        authProvider))
    val status = token.inspect().unsafeRunTimed(10.seconds).value
    assert(status.subject !== "")
  }
}
