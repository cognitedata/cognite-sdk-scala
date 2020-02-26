package com.cognite.sdk.scala.v1

import java.net.UnknownHostException
import java.nio.charset.Charset
import java.time.Instant
import java.util.Base64

import cats.Id
import cats.effect._
import com.cognite.sdk.scala.common.{ApiKeyAuth, Auth, CdpApiException, InvalidAuthentication, LoggingSttpBackend, RetryingBackend, SdkException, SdkTestSpec}
import com.softwaremill.sttp.{Response, SttpBackend}
import com.softwaremill.sttp.asynchttpclient.cats.AsyncHttpClientCatsBackend
import com.softwaremill.sttp.testing.SttpBackendStub

import scala.collection.immutable.Seq
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

class ClientTest extends SdkTestSpec {
  "Client" should "fetch the project using login/status if necessary" in {
    noException should be thrownBy GenericClient.forAuth[Id, Nothing](
      "scala-sdk-test", auth)(
      implicitly,
      sttpBackend
    )
    GenericClient.forAuth[Id, Nothing]("scala-sdk-test", auth)(
      implicitly,
      sttpBackend
    ).projectName should not be empty
  }

  it should "support async IO clients" in {
    implicit val contextShift: ContextShift[IO] = IO.contextShift(ExecutionContext.global)
    implicit val timer: Timer[IO] = IO.timer(ExecutionContext.global)
    GenericClient.forAuth[IO, Nothing]("scala-sdk-test", auth)(
      implicitly,
      new RetryingBackend[IO, Nothing](AsyncHttpClientCatsBackend[IO]())
    ).unsafeRunSync().projectName should not be empty
  }

  it should "throw an exception if the authentication is invalid and project is not specified" in {
    implicit val auth: Auth = ApiKeyAuth("invalid-key")
    an[InvalidAuthentication] should be thrownBy GenericClient.forAuth[Id, Nothing](
      "scala-sdk-test", auth)(
      implicitly,
      sttpBackend
    ).assets.list(Some(1)).compile.toList
  }

  it should "not throw an exception if the authentication is invalid and project is specified" in {
    implicit val auth: Auth = ApiKeyAuth("invalid-key", project = Some("random-project"))
    noException should be thrownBy new GenericClient[Id, Nothing](
      "scala-sdk-test", projectName, auth)(
      implicitly,
      sttpBackend
    )
  }

  it should "be possible to use the sdk with greenfield.cognite.data.com" in {
    implicit val auth: Auth = ApiKeyAuth(Option(System.getenv("TEST_API_KEY_GREENFIELD"))
      .getOrElse(throw new RuntimeException("TEST_API_KEY_GREENFIELD not set")))
    noException should be thrownBy new GenericClient[Id, Nothing](
      "cdp-spark-datasource-test",
      projectName,
      auth,
      "https://greenfield.cognitedata.com"
    )(implicitly,
      sttpBackend
    )
  }

  it should "give a friendly error message when using a malformed base url" in {
    assertThrows[IllegalArgumentException] {
      Client(
        "relationships-unit-tests",
        projectName,
        auth,
        ""
      )(new LoggingSttpBackend[Id, Nothing](sttpBackend)).login.status()
    }
    assertThrows[SdkException] {
      Client(
        "url-test-2",
        projectName,
        auth,
        "api.cognitedata.com"
      )(sttpBackend).login.status()
    }
    assertThrows[UnknownHostException] {
      Client(
        "url-test-3",
        projectName,
        auth,
        "thisShouldThrowAnUnknownHostException:)"
      )(sttpBackend).login.status()
    }
  }

  private val loginStatus = client.login.status()
  private val loginStatusResponse = new Response(Right(
    s"""
       |{
       |  "data": {
       |    "user": "tom@example.com",
       |    "loggedIn": true,
       |    "project": "${loginStatus.project}",
       |    "projectId": "${loginStatus.projectId}",
       |     "projectId": ${loginStatus.projectId}
       |  }
       |}
       |""".stripMargin), 200, "",
    Seq(("x-request-id", "test-request-header"), ("content-type", "application/json; charset=utf-8")),
    Nil)
  private def makeTestingBackend(): SttpBackend[Id, Nothing] = {
    val errorResponse = new Response(Right("{\n  \"error\": {\n    \"code\": 429,\n    \"message\": \"Some error\"\n  }\n}"),
      429, "", Seq(("x-request-id", "test-request-header")), Nil)
    val successResponse = new Response(Right(
      "{\n  \"items\": [\n{\n  \"id\": 5238663994907390,\n  \"createdTime\":" +
        " 1550760030463,\n  \"name\": \"model_793601675501121482\"\n}\n  ]\n}"),
      200, "", Seq(("x-request-id", "test-request-header")), Nil)
    SttpBackendStub.synchronous
      .whenAnyRequest
      .thenRespondCyclicResponses(loginStatusResponse, errorResponse, errorResponse, errorResponse, errorResponse, errorResponse, successResponse
      )
  }

  it should "retry certain failed requests" in {
    assertThrows[CdpApiException] {
      GenericClient.forAuth[Id, Nothing]("scala-sdk-test", auth)(
        implicitly,
        makeTestingBackend()
      ).threeDModels.list()
    }

    val _ = GenericClient.forAuth[Id, Nothing]("scala-sdk-test", auth)(
      implicitly,
      new RetryingBackend[Id, Nothing](
        makeTestingBackend(),
        initialRetryDelay = 1.millis,
        maxRetryDelay = 2.millis)
    ).threeDModels.list()

    assertThrows[CdpApiException] {
      GenericClient.forAuth[Id, Nothing]("scala-sdk-test", auth)(
        implicitly,
        new RetryingBackend[Id, Nothing](
          makeTestingBackend(),
          Some(4),
          initialRetryDelay = 1.millis,
          maxRetryDelay = 2.millis)
      ).threeDModels.list()
    }
  }

  it should "retry requests based on response code if the response is empty" in {
    val badGatewayResponseLeft = new Response(Right(""),
      502, "", Seq.empty, Nil)
    val badGatewayResponseRight = new Response(Right(""),
      502, "", Seq.empty, Nil)
    val unavailableResponse = new Response(Right(""),
      503, "", Seq(("content-type", "application/json; charset=utf-8")), Nil)
    val serverError = new Response(Right(""),
      503, "", Seq(("content-type", "application/protobuf")), Nil)
    val backendStub = SttpBackendStub.synchronous
      .whenAnyRequest
      .thenRespondCyclicResponses(
        badGatewayResponseLeft,
        badGatewayResponseRight,
        unavailableResponse,
        serverError,
        loginStatusResponse)
    val client = new GenericClient[Id, Nothing]("scala-sdk-test",
      projectName,
      ApiKeyAuth("irrelevant", Some("randomproject")),
      "https://www.cognite.com/nowhereatall"
    )(
      implicitly,
      new RetryingBackend[Id, Nothing](backendStub,
        initialRetryDelay = 1.millis,
        maxRetryDelay = 2.millis)
    )
    client.login.status().project shouldBe (loginStatus.project)
  }

  it should "retry JSON requests based on response code if content type is unknown" in {
    val badGatewayResponse: Response[String] = new Response(Left("Bad Gateway".getBytes(Charset.forName("utf-8"))),
      502, "", Seq(("content-type", "text/html")), Nil)
    val unavailableResponse: Response[String]  = new Response(Right("Service Unavailable"),
      503, "", Seq.empty, Nil)
    val serverError: Response[String]  = new Response(Right("Error"),
      503, "", Seq(("content-type", "text/plain")), Nil)
    val serverErrorHtml: Response[String]  = new Response(Right("Error"),
      503, "", Seq(("content-type", "text/html; charset=UTF-8")), Nil)
    val badRequest: Response[String]  = new Response(Right(""),
      503, "", Seq(("content-type", "unknown")), Nil)
    val assetsResponse = new Response(Right(
      s"""
         |{
         |  "items": [{
         |    "name": "some-asset",
         |    "externalId": "ext-123",
         |    "parentId": 123,
         |    "description": "asdf",
         |    "metadata": {},
         |    "source": "test",
         |    "id": 144,
         |    "createdTime": 1546300800000,
         |    "lastUpdatedTime": 1546300800000,
         |    "rootId": 199
         |  }]
         |}
         |""".stripMargin), 200, "",
      Seq(("x-request-id", "test-request-header"), ("content-type", "application/json; charset=utf-8")),
      Nil)
    val badRequestBackendStub = SttpBackendStub.synchronous
      .whenAnyRequest
      .thenRespondCyclicResponses(
        badGatewayResponse,
        unavailableResponse,
        serverError,
        serverErrorHtml,
        badRequest,
        loginStatusResponse,
        badGatewayResponse,
        unavailableResponse,
        serverError,
        serverErrorHtml,
        badRequest,
        assetsResponse
      )
    val client = new GenericClient[Id, Nothing]("scala-sdk-test",
      projectName,
      ApiKeyAuth("irrelevant", Some("randomproject")),
      "https://www.cognite1999.com/nowhere-at-all"
    )(
      implicitly,
      new RetryingBackend[Id, Nothing](
        badRequestBackendStub,
        initialRetryDelay = 1.millis,
        maxRetryDelay = 2.millis)
    )
    client.login.status().project shouldBe loginStatus.project
    client.assets.list().compile.toList.length should be > 0
  }

  it should "retry protobuf requests based on response code if content type is unknown" in {
    val protobufBase64 = "CjYIrt3fh6WwSRIYVkFMXzIzLVBESS05NjE0OTpYLlZhbHVlGhIKEAi9/ta1gC0RAAAAQA0v/T8="
    val protobufResponse: Response[Array[Byte]] = new Response(Right(Base64.getDecoder.decode(protobufBase64)),
      400, "", Seq(("content-type", "application/protobuf")), Nil)

    val badGatewayResponseBytes: Response[Array[Byte]] = new Response(Left("Bad Gateway".getBytes("utf-8")),
      502, "", Seq(("content-type", "text/html")), Nil)
    val unavailableResponseBytes: Response[Array[Byte]]  = new Response(Right("Service Unavailable".getBytes("utf-8")),
      503, "", Seq.empty, Nil)
    val serverErrorBytes: Response[Array[Byte]]  = new Response(Right("Error".getBytes("utf-8")),
      503, "", Seq(("content-type", "text/plain")), Nil)
    val serverErrorHtmlBytes: Response[Array[Byte]]  = new Response(Right("Error".getBytes("utf-8")),
      503, "", Seq(("content-type", "text/html; charset=UTF-8")), Nil)
    val badRequestBytes: Response[Array[Byte]]  = new Response(Right("".getBytes("utf-8")),
      503, "", Seq(("content-type", "unknown")), Nil)
    val badRequestBackendStub1 = SttpBackendStub.synchronous
      .whenAnyRequest
      .thenRespondCyclicResponses(badGatewayResponseBytes,
        unavailableResponseBytes,
        serverErrorBytes,
        serverErrorHtmlBytes,
        badRequestBytes,
        protobufResponse)
    val client2 = new GenericClient[Id, Nothing]("scala-sdk-test",
      projectName,
      ApiKeyAuth("irrelevant", Some("randomproject")),
      "https://www.cognite1999.com/nowhere-at-all"
    )(
      implicitly,
      new RetryingBackend[Id, Nothing](
        badRequestBackendStub1,
        initialRetryDelay = 1.millis,
        maxRetryDelay = 2.millis)
    )
    val points = client2.dataPoints.queryById(123, Instant.ofEpochMilli(1546300800000L), Instant.ofEpochMilli(1546900000000L), None)
    // True value is 1.8239872455596924, but to avoid issues with Scala 2.13 deprecation of
    // double ordering we compare at integer level.
    scala.math.floor(points.datapoints.head.value * 10).toInt shouldBe 18
    scala.math.ceil(points.datapoints.head.value * 10).toInt shouldBe 19
  }
}
