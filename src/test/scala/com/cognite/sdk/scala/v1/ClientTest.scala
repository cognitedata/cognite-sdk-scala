package com.cognite.sdk.scala.v1

import java.net.UnknownHostException

import cats.{Id, Monad}
import com.cognite.sdk.scala.common.{ApiKeyAuth, Auth, CdpApiException, InvalidAuthentication, RetryingBackend, SdkTest}
import com.softwaremill.sttp.Response
import com.softwaremill.sttp.testing.SttpBackendStub

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
    ).projectName should not be empty
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

  it should "retry certain failed requests" in {
    val errorResponse = new Response(Right("{\n  \"error\": {\n    \"code\": 429,\n    \"message\": \"Some error\"\n  }\n}"),
      429, "", scala.collection.immutable.Seq[(String, String)](("x-request-id", "test-request-header")), Nil)
    val successResponse = new Response(Right(
      "{\n  \"items\": [\n{\n  \"id\": 5238663994907390,\n  \"createdTime\":" +
        " 1550760030463,\n  \"name\": \"model_793601675501121482\"\n}\n  ]\n}"),
      200, "", scala.collection.immutable.Seq[(String, String)](("x-request-id", "test-request-header")), Nil)

    val testingBackend = SttpBackendStub.synchronous
      .whenAnyRequest
      .thenRespondCyclicResponses(errorResponse, errorResponse, errorResponse, errorResponse, errorResponse, successResponse
      )

    assertThrows[CdpApiException] {
      new GenericClient("scala-sdk-test")(
        implicitly[Monad[Id]],
        auth,
        testingBackend
      ).threeDModels.list()
    }

    val _ = new GenericClient("scala-sdk-test")(
      implicitly[Monad[Id]],
      auth,
      new RetryingBackend[Id, Nothing](testingBackend)
    ).threeDModels.list()

    assertThrows[CdpApiException] {
      new GenericClient("scala-sdk-test")(
      implicitly[Monad[Id]],
      auth,
      new RetryingBackend[Id, Nothing](testingBackend, Some(4))
      ).threeDModels.list()
    }
  }
}
