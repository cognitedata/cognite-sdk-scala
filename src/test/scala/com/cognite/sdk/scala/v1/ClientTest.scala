// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1

import BuildInfo.BuildInfo

import java.net.UnknownHostException
import java.time.Instant
import java.util.Base64
import cats.Id
import cats.catsInstancesForId
import cats.effect._
import cats.effect.laws.util.TestContext
import com.cognite.sdk.scala.common.{ApiKeyAuth, Auth, CdpApiException, InvalidAuthentication, LoggingSttpBackend, RetryingBackend, SdkException, SdkTestSpec}
import sttp.client3.{Response, SttpBackend}
import sttp.client3.asynchttpclient.cats.AsyncHttpClientCatsBackend
import sttp.client3.testing.SttpBackendStub
import sttp.model.{Header, StatusCode}

import java.util.concurrent.Executors
import scala.collection.immutable.Seq
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

@SuppressWarnings(Array("org.wartremover.warts.NonUnitStatements", "org.wartremover.warts.Var"))
class ClientTest extends SdkTestSpec {
  private val loginStatus = client.login.status()
  private val loginStatusResponse = Response(
    s"""
       |{
       |  "data": {
       |    "user": "tom@example.com",
       |    "loggedIn": true,
       |    "project": "${loginStatus.project}",
       |    "projectId": ${loginStatus.projectId.toString}
       |  }
       |}
       |""".stripMargin, StatusCode.Ok, "OK",
    Seq(Header("x-request-id", "test-request-header"), Header("content-type", "application/json; charset=utf-8")))
  private def makeTestingBackend(): SttpBackend[Id, Any] = {
    val errorResponse = Response("{\n  \"error\": {\n    \"code\": 429,\n    \"message\": \"Some error\"\n  }\n}",
      StatusCode.TooManyRequests, "", Seq(Header("x-request-id", "test-request-header")))
    val successResponse = Response(
      "{\n  \"items\": [\n{\n  \"id\": 5238663994907390,\n  \"createdTime\":" +
        " 1550760030463,\n  \"name\": \"model_793601675501121482\"\n}\n  ]\n}",
      StatusCode.Ok, "", Seq(Header("x-request-id", "test-request-header")))
    SttpBackendStub.synchronous
      .whenAnyRequest
      .thenRespondCyclicResponses(loginStatusResponse, errorResponse, errorResponse, errorResponse, errorResponse, errorResponse, successResponse
      )
  }
  private val loginStatusResponseWithApiKeyId = Response(
      s"""
         |{
         |  "data": {
         |    "user": "tom@example.com",
         |    "loggedIn": true,
         |    "project": "${loginStatus.project}",
         |    "projectId": ${loginStatus.projectId.toString},
         |    "apiKeyId": 12147483647
         |  }
         |}
         |""".stripMargin, StatusCode.Ok, "OK",
      Seq(Header("x-request-id", "test-request-header"), Header("content-type", "application/json; charset=utf-8")))

  "Client" should "fetch the project using login/status if necessary" in {
    noException should be thrownBy GenericClient.forAuth[Id](
      "scala-sdk-test", auth)(
      implicitly,
      sttpBackend
    )
    GenericClient.forAuth[Id]("scala-sdk-test", auth)(
      implicitly,
      sttpBackend
    ).projectName should not be empty
  }

  it should "not require apiKeyId to be present" in {
    val loginStatusResponseWithoutApiKeyId = Response(
      s"""
         |{
         |  "data": {
         |    "user": "",
         |    "loggedIn": false,
         |    "project": "",
         |    "projectId": -1
         |  }
         |}
         |""".stripMargin, StatusCode.Ok, "OK",
      Seq(Header("x-request-id", "test-request-header"), Header("content-type", "application/json; charset=utf-8")))

    val respondWithoutApiKeyId = SttpBackendStub.synchronous
      .whenAnyRequest
      .thenRespond(loginStatusResponseWithoutApiKeyId)
    new GenericClient[Id](
      "scala-sdk-test", projectName, auth = auth)(
      implicitly,
      respondWithoutApiKeyId
    ).login.status().apiKeyId shouldBe empty
  }

  it should "handle an apiKeyId which is larger than an int" in {
    val respondWithApiKeyId = SttpBackendStub.synchronous
      .whenAnyRequest
      .thenRespond(loginStatusResponseWithApiKeyId)
    new GenericClient[Id](
      "scala-sdk-test", projectName, auth = auth)(
      implicitly,
      respondWithApiKeyId
    ).login.status().apiKeyId shouldBe Some(12147483647L)
  }

  it should "set x-cdp headers" in {
    var headers = Seq.empty[Header]
    val saveHeadersStub = SttpBackendStub.synchronous
      .whenAnyRequest
      .thenRespondF { req =>
        headers = req.headers
        Response.ok(loginStatusResponseWithApiKeyId).copy(headers = req.headers)
      }
    new GenericClient[Id]("scala-sdk-test", projectName, auth = auth, clientTag = Some("client-test"))(implicitly, saveHeadersStub)
      .login.status()
    headers should contain (Header("x-cdp-clienttag", "client-test"))
    headers should contain (Header("x-cdp-sdk", s"CogniteScalaSDK:${BuildInfo.version}"))
    headers should contain (Header("x-cdp-app", "scala-sdk-test"))
  }

  it should "support async IO clients" in {
    implicit val testContext: TestContext = TestContext()
    implicit val cs: ContextShift[IO] = IO.contextShift(ExecutionContext.fromExecutor(Executors.newFixedThreadPool(4)))
    implicit val timer: Timer[IO] = testContext.timer[IO]
    GenericClient.forAuth[IO]("scala-sdk-test", auth)(
      implicitly,
      new RetryingBackend[IO, Any](AsyncHttpClientCatsBackend[IO]().unsafeRunSync())
    ).unsafeRunSync().projectName should not be empty
  }

  it should "throw an exception if the authentication is invalid and project is not specified" in {
    implicit val auth: Auth = ApiKeyAuth("invalid-key")
    an[InvalidAuthentication] should be thrownBy GenericClient.forAuth[Id](
      "scala-sdk-test", auth)(
      implicitly,
      sttpBackend
    ).assets.list(Some(1)).compile.toList
  }

  it should "not throw an exception if the authentication is invalid and project is specified" in {
    implicit val auth: Auth = ApiKeyAuth("invalid-key", project = Some("random-project"))
    noException should be thrownBy new GenericClient[Id](
      "scala-sdk-test", projectName, auth = auth)(
      implicitly,
      sttpBackend
    )
  }

  it should "give a friendly error message when using a malformed base url" in {
    assertThrows[IllegalArgumentException] {
      Client(
        "relationships-unit-tests",
        projectName,
        "",
        auth
      )(new LoggingSttpBackend[Id, Any](sttpBackend)).login.status()
    }
    assertThrows[SdkException] {
      Client(
        "url-test-2",
        projectName,
        "http://api.cognitedata.com",
        auth
      )(sttpBackend).login.status()
    }
    assertThrows[UnknownHostException] {
      Client(
        "url-test-3",
        projectName,
        "thisShouldThrowAnUnknownHostException:)",
        auth
      )(sttpBackend).login.status()
    }
  }

  it should "retry certain failed requests" in {
    assertThrows[CdpApiException] {
      GenericClient.forAuth[Id]("scala-sdk-test", auth)(
        implicitly,
        makeTestingBackend()
      ).threeDModels.list()
    }

    val _ = GenericClient.forAuth[Id]("scala-sdk-test", auth)(
      implicitly,
      new RetryingBackend[Id, Any](
        makeTestingBackend(),
        initialRetryDelay = 1.millis,
        maxRetryDelay = 2.millis)
    ).threeDModels.list()

    assertThrows[CdpApiException] {
      GenericClient.forAuth[Id]("scala-sdk-test", auth)(
        implicitly,
        new RetryingBackend[Id, Any](
          makeTestingBackend(),
          Some(4),
          initialRetryDelay = 1.millis,
          maxRetryDelay = 2.millis)
      ).threeDModels.list()
    }
  }

  it should "retry requests based on response code if the response is empty" in {
    val badGatewayResponseLeft = Response("",
      StatusCode.BadGateway, "", Seq.empty)
    val badGatewayResponseRight = Response("",
      StatusCode.BadGateway, "", Seq.empty)
    val unavailableResponse = Response("",
      StatusCode.ServiceUnavailable, "", Seq(Header("content-type", "application/json; charset=utf-8")))
    val serverError = Response("",
      StatusCode.ServiceUnavailable, "", Seq(Header("content-type", "application/protobuf")))
    val backendStub = SttpBackendStub.synchronous
      .whenAnyRequest
      .thenRespondCyclicResponses(
        badGatewayResponseLeft,
        badGatewayResponseRight,
        unavailableResponse,
        serverError,
        loginStatusResponse)
    val client = new GenericClient[Id]("scala-sdk-test",
      projectName,
      "https://www.cognite.com/nowhereatall",
      ApiKeyAuth("irrelevant", Some("randomproject"))

    )(
      implicitly,
      new RetryingBackend[Id, Any](backendStub,
        initialRetryDelay = 1.millis,
        maxRetryDelay = 2.millis)
    )
    client.login.status().project shouldBe (loginStatus.project)
  }

  it should "retry JSON requests based on response code if content type is unknown" in {
    val badGatewayResponse: Response[String] = Response("Bad Gateway",
      StatusCode.BadGateway, "", Seq(Header("content-type", "text/html")))
    val unavailableResponse: Response[String]  = Response("Service Unavailable",
      StatusCode.ServiceUnavailable, "", Seq.empty)
    val serverError: Response[String]  = Response("Error",
      StatusCode.ServiceUnavailable, "", Seq(Header("content-type", "text/plain")))
    val serverErrorHtml: Response[String]  = Response("Error",
      StatusCode.ServiceUnavailable, "", Seq(Header("content-type", "text/html; charset=UTF-8")))
    val badRequest: Response[String]  = Response("",
      StatusCode.ServiceUnavailable, "", Seq(Header("content-type", "unknown")))
    val assetsResponse = Response(
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
         |""".stripMargin, StatusCode.Ok, "OK",
      Seq(Header("x-request-id", "test-request-header"), Header("content-type", "application/json; charset=utf-8")))
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
    val client = new GenericClient[Id]("scala-sdk-test",
      projectName,
      "https://www.cognite.com/nowhere-at-all",
      ApiKeyAuth("irrelevant", Some("randomproject"))
    )(
      implicitly,
      new RetryingBackend[Id, Any](
        badRequestBackendStub,
        initialRetryDelay = 1.millis,
        maxRetryDelay = 2.millis)
    )
    client.login.status().project shouldBe loginStatus.project
    client.assets.list().compile.toList.length should be > 0
  }

  it should "retry protobuf requests based on response code if content type is unknown" in {
    val protobufBase64 = "CjYIrt3fh6WwSRIYVkFMXzIzLVBESS05NjE0OTpYLlZhbHVlGhIKEAi9/ta1gC0RAAAAQA0v/T8="
    val protobufResponse: Response[Array[Byte]] = Response(Base64.getDecoder.decode(protobufBase64),
      StatusCode.Ok, "", Seq(Header("content-type", "application/protobuf")))

    val badGatewayResponseBytes: Response[Array[Byte]] = Response("Bad Gateway".getBytes("utf-8"),
      StatusCode.BadGateway, "", Seq(Header("content-type", "text/html")))
    val unavailableResponseBytes: Response[Array[Byte]]  = Response("Service Unavailable".getBytes("utf-8"),
      StatusCode.ServiceUnavailable, "", Seq.empty)
    val serverErrorBytes: Response[Array[Byte]]  = Response("Error".getBytes("utf-8"),
      StatusCode.ServiceUnavailable, "", Seq(Header("content-type", "text/plain")))
    val serverErrorHtmlBytes: Response[Array[Byte]]  = Response("Error".getBytes("utf-8"),
      StatusCode.ServiceUnavailable, "", Seq(Header("content-type", "text/html; charset=UTF-8")))
    val badRequestBytes: Response[Array[Byte]]  = Response("".getBytes("utf-8"),
      StatusCode.ServiceUnavailable, "", Seq(Header("content-type", "unknown")))
    val badRequestBackendStub1 = SttpBackendStub.synchronous
      .whenAnyRequest
      .thenRespondCyclicResponses(badGatewayResponseBytes,
        unavailableResponseBytes,
        serverErrorBytes,
        serverErrorHtmlBytes,
        badRequestBytes,
        protobufResponse)
    val client2 = new GenericClient[Id]("scala-sdk-test",
      projectName,
      "https://www.cognite.com/nowhere-at-all",
      ApiKeyAuth("irrelevant", Some("randomproject"))
    )(
      implicitly,
      new RetryingBackend[Id, Any](
        badRequestBackendStub1,
        initialRetryDelay = 1.millis,
        maxRetryDelay = 2.millis)
    )
    val points = client2.dataPoints.queryById(123, Instant.ofEpochMilli(1546300800000L), Instant.ofEpochMilli(1546900000000L), None)
    // True value is 1.8239872455596924, but to avoid issues with Scala 2.13 deprecation of
    // double ordering we compare at integer level.
    scala.math.floor(points.datapoints(0).value * 10).toInt shouldBe 18
    scala.math.ceil(points.datapoints(0).value * 10).toInt shouldBe 19
  }
}
