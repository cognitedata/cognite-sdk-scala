package com.cognite.sdk.scala.v1

import com.cognite.sdk.scala.common.SdkTestSpec
import org.scalatest.{EitherValues, OptionValues}
import sttp.client3.UriContext

class RequestSessionTest extends SdkTestSpec with OptionValues with EitherValues {
  it should "send a head request and return the headers" in {
    client.requestSession.head(uri"https://www.cognite.com/").unsafeRunSync() should not be(empty)
  }
}
