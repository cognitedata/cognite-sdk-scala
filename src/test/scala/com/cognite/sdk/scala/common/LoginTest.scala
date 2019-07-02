package com.cognite.sdk.scala.common

import com.softwaremill.sttp.{HttpURLConnectionBackend, Id, SttpBackend, SttpBackendOptions}

import scala.concurrent.duration._

class LoginTest extends SdkTest {
  implicit val backend: SttpBackend[Id, Nothing] = HttpURLConnectionBackend(
    options = SttpBackendOptions.connectionTimeout(90.seconds)
  )
  it should "read login status" in {
    val login = new Login()
    val status = login.status().unsafeBody
    status.loggedIn should be (true)
    status.project should not be empty
  }
}
