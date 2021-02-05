//// Copyright 2020 Cognite AS
//// SPDX-License-Identifier: Apache-2.0
//
//package com.cognite.sdk.scala.common
//
//import com.cognite.sdk.scala.v1.RequestSession
//import com.softwaremill.sttp._
//
//import scala.concurrent.duration._
//
//class LoginTest extends SdkTestSpec {
//  implicit val backend: SttpBackend[Id, Nothing] = HttpURLConnectionBackend(
//    options = SttpBackendOptions.connectionTimeout(90.seconds)
//  )
//  it should "read login status" in {
//    val login =
//      new Login(RequestSession[Id]("scala-sdk-test", uri"https://api.cognitedata.com", backend, AuthProvider[Id](auth)))
//    val status = login.status()
//    status.loggedIn should be(true)
//    status.project should not be empty
//  }
//}
