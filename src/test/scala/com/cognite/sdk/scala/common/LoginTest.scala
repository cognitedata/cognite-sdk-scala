package com.cognite.sdk.scala.common

import com.cognite.sdk.scala.v1.RequestSession
import com.softwaremill.sttp._

import scala.concurrent.duration._

class LoginTest extends SdkTest {
  implicit val backend: SttpBackend[Id, Nothing] = HttpURLConnectionBackend(
    options = SttpBackendOptions.connectionTimeout(90.seconds)
  )
  it should "read login status" in {
    val login = new Login(RequestSession(uri"https://api.cognitedata.com", backend, auth))
    val status = login.status()
    status.loggedIn should be (true)
    status.project should not be empty
  }
}
