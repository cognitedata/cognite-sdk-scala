package com.cognite.sdk.scala.v1

import com.cognite.sdk.scala.common.{LoggingSttpBackend, SdkTestSpec}
import org.scalatest.{EitherValues, OptionValues}
import sttp.client3.SttpClientException

import java.net.UnknownHostException

@SuppressWarnings(Array("org.wartremover.warts.NonUnitStatements", "org.wartremover.warts.Var"))
class SyncClientTest extends SdkTestSpec with OptionValues with EitherValues {
  import SyncClient.sttpBackend

  it should "give a friendly error message when using a malformed base url" in {
    assertThrows[IllegalArgumentException] {
      SyncClient(
        "relationships-unit-tests",
        projectName,
        "",
        auth
      )(implicitly, new LoggingSttpBackend[OrError, Any](sttpBackend)).token.inspect()
    }
    assertThrows[UnknownHostException] {
      SyncClient(
        "url-test-3",
        projectName,
        "thisShouldThrowAnUnknownHostException:)",
        auth
      )(implicitly, sttpBackend).token.inspect()
    }
  }

  it should "throw an SttpClientException when using plain http" in {
    val error = {
      SyncClient(
        "url-test-2",
        projectName,
        "http://api.cognitedata.com",
        auth
      )(implicitly, sttpBackend).token.inspect()
    }.left.value
    error shouldBe a [SttpClientException]
  }


}
