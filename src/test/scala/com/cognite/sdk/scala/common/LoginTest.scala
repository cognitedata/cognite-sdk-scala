package com.cognite.sdk.scala.common

class LoginTest extends SdkTest {
  it should "read login status" in {
    val login = new Login()
    val status = login.status().unsafeBody
    status.loggedIn should be (true)
    status.project should not be empty
  }
}
