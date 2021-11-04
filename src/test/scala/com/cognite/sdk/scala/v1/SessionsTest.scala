// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1

import cats.Id
import com.cognite.sdk.scala.common._
import sttp.client3._
import sttp.client3.testing.SttpBackendStub
import sttp.model.{Header, MediaType, Method, StatusCode}

import java.time.Instant
import scala.collection.immutable.Seq

@SuppressWarnings(
  Array("org.wartremover.warts.TraversableOps", "org.wartremover.warts.NonUnitStatements")
)
class SessionsTest extends SdkTestSpec with ReadBehaviours {

  "Sessions" should "create a new session with credential flow" in {
    val expectedResponse = Seq(Session(0, "CLIENT_CREDENTIALS", "READY", "nonce", "clientId"))
    val responseForSessionCreated = SttpBackendStub.synchronous
      .whenRequestMatches { r =>
        r.method === Method.POST && r.uri.path.endsWith(List("sessions")) && r.body === StringBody(
          """{"items":[{"clientId":"clientId","clientSecret":"clientSecret"}]}""",
          "utf-8",
          MediaType.ApplicationJson
        )
      }
      .thenRespond(
        Response(
          expectedResponse,
          StatusCode.Ok,
          "OK",
          Seq(Header("content-type", "application/json; charset=utf-8"))
        )
      )

    val client = new GenericClient[Id](
      applicationName = "CogniteScalaSDK-OAuth-Test",
      projectName = "session-testing",
      auth = BearerTokenAuth("bearer Token")
    )(implicitly, responseForSessionCreated)

    val resCreate = client.sessions.createWithClientCredentialFlow(
      Items[SessionCreateWithCredential](
        Seq(SessionCreateWithCredential("clientId", "clientSecret"))
      )
    )
    resCreate shouldBe expectedResponse
  }

  it should "create a new session with token exchange flow" in {
    val expectedResponse = Seq(Session(0, "TOKEN_EXCHANGE", "READY", "nonce", "clientId"))
    val responseForSessionCreated = SttpBackendStub.synchronous
      .whenRequestMatches { r =>
        r.method === Method.POST && r.uri.path.endsWith(List("sessions")) && r.body === StringBody(
          """{"items":[{"tokenExchange":true}]}""",
          "utf-8",
          MediaType.ApplicationJson
        )
      }
      .thenRespond(
        Response(
          expectedResponse,
          StatusCode.Ok,
          "OK",
          Seq(Header("content-type", "application/json; charset=utf-8"))
        )
      )

    val client = new GenericClient[Id](
      applicationName = "CogniteScalaSDK-OAuth-Test",
      projectName = "session-testing",
      auth = BearerTokenAuth("bearer Token")
    )(implicitly, responseForSessionCreated)

    val resCreate = client.sessions.createWithTokenExchangeFlow(
      Items[SessionCreateWithToken](
        Seq(SessionCreateWithToken(true))
      )
    )
    resCreate shouldBe expectedResponse
  }

  it should "fail to create a new session if input items is empty" in {
    val expectedError = """
        {
            "error": {
                "code": 400,
                "message": "Request must contain exactly 1 item in request body"
            }
        }
        """
    val responseForSessionCreated = SttpBackendStub.synchronous
      .whenRequestMatches { r =>
        r.method === Method.POST && r.uri.path.endsWith(List("sessions")) && r.body === StringBody(
          """{"items":[]}""",
          "utf-8",
          MediaType.ApplicationJson
        )
      }
      .thenRespond(
        Response(
          expectedError,
          StatusCode.BadRequest,
          "BadRequest",
          Seq(Header("content-type", "application/json; charset=utf-8"))
        )
      )

    val client = new GenericClient[Id](
      applicationName = "CogniteScalaSDK-OAuth-Test",
      projectName = "session-testing",
      auth = BearerTokenAuth("bearer Token")
    )(implicitly, responseForSessionCreated)

    val error = the[CdpApiException] thrownBy client.sessions.createWithClientCredentialFlow(
      Items[SessionCreateWithCredential](
        Seq[SessionCreateWithCredential]()
      )
    )
    error.message shouldBe s"Request must contain exactly 1 item in request body"
  }

  it should "fail to create new session with invalid credential" in {
    val expectedError = """
        {
            "error": {
                "code": 403,
                "message": "Resource not found. This may also be due to insufficient access rights."
            }
        }
        """
    val responseForSessionCreated = SttpBackendStub.synchronous
      .whenRequestMatches { r =>
        r.method === Method.POST && r.uri.path.endsWith(List("sessions")) && r.body === StringBody(
          """{"items":[{"clientId":"clientId","clientSecret":"clientSecret"}]}""",
          "utf-8",
          MediaType.ApplicationJson
        )
      }
      .thenRespond(
        Response(
          expectedError,
          StatusCode.Forbidden,
          "BadRequest",
          Seq(Header("content-type", "application/json; charset=utf-8"))
        )
      )

    val client = new GenericClient[Id](
      applicationName = "CogniteScalaSDK-OAuth-Test",
      projectName = "session-testing",
      auth = BearerTokenAuth("bearer Token")
    )(implicitly, responseForSessionCreated)

    val error = the[CdpApiException] thrownBy client.sessions.createWithClientCredentialFlow(
      Items[SessionCreateWithCredential](
        Seq(SessionCreateWithCredential("clientId", "clientSecret"))
      )
    )
    error.message shouldBe s"Resource not found. This may also be due to insufficient access rights."
  }

  it should "list all the sessions" in {
    val expectedResponse = Seq(
      SessionList(
        1,
        "CLIENT_CREDENTIALS",
        "READY",
        Instant.now().toEpochMilli,
        Instant.now().plusSeconds(60).toEpochMilli,
        "clientId"
      ),
      SessionList(
        2,
        "CLIENT_CREDENTIALS",
        "CANCELLED",
        Instant.now().minusSeconds(120).toEpochMilli,
        Instant.now().minusSeconds(60).toEpochMilli,
        "clientId"
      )
    )
    val responseForSessionList = SttpBackendStub.synchronous
      .whenRequestMatches(r => r.method === Method.GET && r.uri.path.endsWith(List("sessions")))
      .thenRespond(
        Response(
          expectedResponse,
          StatusCode.Ok,
          "OK",
          Seq(Header("content-type", "application/json; charset=utf-8"))
        )
      )

    val client = new GenericClient[Id](
      applicationName = "CogniteScalaSDK-OAuth-Test",
      projectName = "session-testing",
      auth = BearerTokenAuth("bearer Token")
    )(implicitly, responseForSessionList)

    val responseList = client.sessions.list()
    responseList.size shouldBe 2
    responseList shouldBe expectedResponse
  }

  it should "bind a session" in {
    val expectedResponse = SessionTokenResponse(
      1,
      "accessToken",
      Instant.now().toEpochMilli,
      Instant.now().plusSeconds(60).toEpochMilli,
      Some("sessionKey")
    )
    val responseForSessionList = SttpBackendStub.synchronous
      .whenRequestMatches { r =>
        r.method === Method.POST && r.uri.path.endsWith(
          List("sessions", "token")
        ) && r.body === StringBody(
          """{"nonce":"nonce-value"}""",
          "utf-8",
          MediaType.ApplicationJson
        )
      }
      .thenRespond(
        Response(
          expectedResponse,
          StatusCode.Ok,
          "OK",
          Seq(Header("content-type", "application/json; charset=utf-8"))
        )
      )

    val client = new GenericClient[Id](
      applicationName = "CogniteScalaSDK-OAuth-Test",
      projectName = "session-testing",
      auth = BearerTokenAuth("bearer Token")
    )(implicitly, responseForSessionList)

    val responseBind = client.sessions.bind(BindSessionRequest("nonce-value"))
    responseBind shouldBe expectedResponse
  }

  it should "refresh a session" in {
    val expectedResponse = SessionTokenResponse(
      1,
      "accessToken",
      Instant.now().toEpochMilli,
      Instant.now().plusMillis(60000).toEpochMilli,
      Some("sessionKey")
    )
    val responseForSessionList = SttpBackendStub.synchronous
      .whenRequestMatches { r =>
        r.method === Method.POST && r.uri.path.endsWith(
          List("sessions", "token")
        ) && r.body === StringBody(
          """{"sessionKey":"sessionKey-value"}""",
          "utf-8",
          MediaType.ApplicationJson
        )
      }
      .thenRespond(
        Response(
          expectedResponse,
          StatusCode.Ok,
          "OK",
          Seq(Header("content-type", "application/json; charset=utf-8"))
        )
      )

    val client = new GenericClient[Id](
      applicationName = "CogniteScalaSDK-OAuth-Test",
      projectName = "session-testing",
      auth = BearerTokenAuth("bearer Token")
    )(implicitly, responseForSessionList)

    val responseBind = client.sessions.refresh(RefreshSessionRequest("sessionKey-value"))
    responseBind shouldBe expectedResponse
  }

}
