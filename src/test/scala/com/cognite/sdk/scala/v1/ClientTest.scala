package com.cognite.sdk.scala.v1

import java.net.UnknownHostException

import cats.{Id, Monad}
import com.cognite.sdk.scala.common.{ApiKeyAuth, Auth, InvalidAuthentication, SdkTest}

class ClientTest extends SdkTest {
  "Client" should "fetch the project using login/status if necessary" in {
    noException should be thrownBy new GenericClient(
      "scala-sdk-test")(
      implicitly[Monad[Id]],
      auth,
      sttpBackend
    )
    new GenericClient("scala-sdk-test")(
      implicitly[Monad[Id]],
      auth,
      sttpBackend
    ).project should not be empty
  }
  it should "throw an exception if the authentication is invalid and project is not specified" in {
    implicit val auth: Auth = ApiKeyAuth("invalid-key")
    an[InvalidAuthentication] should be thrownBy new GenericClient(
      "scala-sdk-test")(
      implicitly[Monad[Id]],
      auth,
      sttpBackend
    )
  }
  it should "not throw an exception if the authentication is invalid and project is specified" in {
    implicit val auth: Auth = ApiKeyAuth("invalid-key", project = Some("random-project"))
    noException should be thrownBy new GenericClient(
      "scala-sdk-test")(
      implicitly[Monad[Id]],
      auth,
      sttpBackend
    )
  }

  it should "be possible to use the sdk with greenfield.cognite.data.com" in {
    implicit val auth: Auth = ApiKeyAuth(Option(System.getenv("TEST_API_KEY_GREENFIELD"))
      .getOrElse(throw new RuntimeException("TEST_API_KEY_GREENFIELD not set")))
    noException should be thrownBy new GenericClient(
      "cdp-spark-datasource-test",
      "https://greenfield.cognitedata.com"
    )(implicitly[Monad[Id]],
      auth,
      sttpBackend
    )
  }

  it should "give a friendly error message when using a malformed base url" in {
    assertThrows[IllegalArgumentException] {
      Client(
        "relationships-unit-tests",
        ""
      )(auth, sttpBackend)
    }
    assertThrows[RuntimeException] {
      Client(
        "url-test-2",
        "api.cognitedata.com"
      )(auth, sttpBackend)
    }
    assertThrows[UnknownHostException] {
      Client(
        "url-test-3",
        "thisShouldThrowAnUnknownHostException:)"
      )(auth, sttpBackend)
    }
  }
}
