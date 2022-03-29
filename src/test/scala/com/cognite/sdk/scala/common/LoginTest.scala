// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.common

import cats.Id
import com.cognite.sdk.scala.v1.RequestSession
import sttp.client3._

import scala.concurrent.duration._

class LoginTest extends SdkTestSpec {
  implicit val backend: SttpBackend[Id, Any] = HttpURLConnectionBackend(
    options = SttpBackendOptions.connectionTimeout(90.seconds)
  )
  it should "read login status" in {
    val login =
      new Login(RequestSession[Id]("scala-sdk-test", uri"https://api.cognitedata.com", backend, AuthProvider[Id](auth)))
    val status = login.status()
    assert(status.loggedIn)
    assert(status.project === "some-project")
    status.project should not be empty
  }
}
