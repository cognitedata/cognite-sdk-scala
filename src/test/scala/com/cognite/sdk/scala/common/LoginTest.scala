package com.cognite.sdk.scala.common

import com.cognite.sdk.scala.v0_6.Client

class LoginTest extends SdkTest {
  it should "read login status" in {
    val client = new Client()
    val status = client.login.status()
    println(status.unsafeBody) // scalastyle:ignore
  }
}
