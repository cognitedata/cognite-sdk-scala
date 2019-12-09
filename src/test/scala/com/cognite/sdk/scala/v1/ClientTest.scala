package com.cognite.sdk.scala.v1

import java.net.UnknownHostException

import cats.{Comonad, Id, Monad}
import cats.effect._
import com.cognite.sdk.scala.common.{ApiKeyAuth, Auth, CdpApiException, InvalidAuthentication, RetryingBackend, SdkException, SdkTest}
import com.softwaremill.sttp.Response
import com.softwaremill.sttp.asynchttpclient.cats.AsyncHttpClientCatsBackend
import com.softwaremill.sttp.testing.SttpBackendStub

import scala.concurrent.ExecutionContext

class ClientTest extends SdkTest {
  "Client" should "fetch the project using login/status if necessary" in {
    noException should be thrownBy new GenericClient(
      "scala-sdk-test")(
      implicitly[Monad[Id]],
      implicitly[Comonad[Id]],
      auth,
      sttpBackend
    )
    new GenericClient("scala-sdk-test")(
      implicitly[Monad[Id]],
      implicitly[Comonad[Id]],
      auth,
      sttpBackend
    ).projectName should not be empty
  }
  it should "support async IO clients" in {
    implicit val contextShift: ContextShift[IO] = IO.contextShift(ExecutionContext.global)
    implicit val timer: Timer[IO] = IO.timer(ExecutionContext.global)
    new GenericClient[IO, Nothing]("scala-sdk-test")(
      implicitly[Monad[IO]],
      implicitly[Comonad[IO]],
      auth,
      new RetryingBackend[IO, Nothing](AsyncHttpClientCatsBackend[IO]())
    ).projectName should not be empty
  }
  it should "throw an exception if the authentication is invalid and project is not specified" in {
    implicit val auth: Auth = ApiKeyAuth("invalid-key")
    an[InvalidAuthentication] should be thrownBy new GenericClient(
      "scala-sdk-test")(
      implicitly[Monad[Id]],
      implicitly[Comonad[Id]],
      auth,
      sttpBackend
    ).assets.list(Some(1)).compile.toList
  }
  it should "not throw an exception if the authentication is invalid and project is specified" in {
    implicit val auth: Auth = ApiKeyAuth("invalid-key", project = Some("random-project"))
    noException should be thrownBy new GenericClient(
      "scala-sdk-test")(
      implicitly[Monad[Id]],
      implicitly[Comonad[Id]],
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
      implicitly[Comonad[Id]],
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
    assertThrows[SdkException] {
      Client(
        "url-test-2",
        "api.cognitedata.com"
      )(auth, sttpBackend).projectName
    }
    assertThrows[UnknownHostException] {
      Client(
        "url-test-3",
        "thisShouldThrowAnUnknownHostException:)"
      )(auth, sttpBackend).projectName
    }
  }

  private val loginStatus = client.login.status()
  private def makeTestingBackend() = {
    val errorResponse = new Response(Right("{\n  \"error\": {\n    \"code\": 429,\n    \"message\": \"Some error\"\n  }\n}"),
      429, "", scala.collection.immutable.Seq[(String, String)](("x-request-id", "test-request-header")), Nil)
    val successResponse = new Response(Right(
      "{\n  \"items\": [\n{\n  \"id\": 5238663994907390,\n  \"createdTime\":" +
        " 1550760030463,\n  \"name\": \"model_793601675501121482\"\n}\n  ]\n}"),
      200, "", scala.collection.immutable.Seq[(String, String)](("x-request-id", "test-request-header")), Nil)
    val loginStatusResponse = new Response(Right(
      s"""
         |{
         |  "data": {
         |    "user": "tom@example.com",
         |    "loggedIn": true,
         |    "project": "${loginStatus.project}",
         |    "projectId": ${loginStatus.projectId}
         |  }
         |}
         |""".stripMargin), 200, "",
      scala.collection.immutable.Seq[(String, String)](("x-request-id", "test-request-header")),
      Nil)
    SttpBackendStub.synchronous
      .whenAnyRequest
      .thenRespondCyclicResponses(loginStatusResponse, errorResponse, errorResponse, errorResponse, errorResponse, errorResponse, successResponse
      )
  }
  it should "retry certain failed requests" in {
    assertThrows[CdpApiException] {
      new GenericClient("scala-sdk-test")(
        implicitly[Monad[Id]],
        implicitly[Comonad[Id]],
        auth,
        makeTestingBackend()
      ).threeDModels.list()
    }

    val _ = new GenericClient("scala-sdk-test")(
      implicitly[Monad[Id]],
      implicitly[Comonad[Id]],
      auth,
      new RetryingBackend[Id, Nothing](makeTestingBackend())
    ).threeDModels.list()

    assertThrows[CdpApiException] {
      new GenericClient("scala-sdk-test")(
        implicitly[Monad[Id]],
        implicitly[Comonad[Id]],
        auth,
        new RetryingBackend[Id, Nothing](makeTestingBackend(), Some(4))
      ).threeDModels.list()
    }
  }
}
