// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1

import cats.effect.IO
import com.cognite.sdk.scala.common._
import sttp.client3._
import sttp.client3.testing.SttpBackendStub
import sttp.client3.impl.cats.implicits.asyncMonadError
import sttp.model.{Header, MediaType, Method, StatusCode}
import sttp.monad.EitherMonad

import java.time.Instant
import scala.collection.immutable.Seq

import io.circe.syntax._
import io.circe.Encoder
import io.circe.generic.semiauto.deriveEncoder
import org.scalatest.{EitherValues, Inside}

@SuppressWarnings(
  Array("org.wartremover.warts.TraversableOps", "org.wartremover.warts.NonUnitStatements")
)
class SessionsTest extends SdkTestSpec with ReadBehaviours with EitherValues with Inside {
  "Sessions" should "create a new session with credential flow" in {
    val expectedResponse =
      Seq(Session(0, Some("CLIENT_CREDENTIALS"), "READY", "nonce", Some("clientId")))
    val responseForSessionCreated = SttpBackendStub(EitherMonad)
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

    val client = new GenericClient[OrError](
      applicationName = "CogniteScalaSDK-OAuth-Test",
      projectName = "session-testing",
      auth = BearerTokenAuth("bearer Token")
    )(implicitly, implicitly, responseForSessionCreated)

    val resCreate = client.sessions
      .createWithClientCredentialFlow(
        Items[SessionCreateWithCredential](
          Seq(SessionCreateWithCredential("clientId", "clientSecret"))
        )
      )
    resCreate shouldBe Right(expectedResponse)
  }

  it should "create a new session with token exchange flow" in {
    val expectedResponse = Seq(Session(0, Some("TOKEN_EXCHANGE"), "READY", "nonce", None))
    val responseForSessionCreated = SttpBackendStub(EitherMonad)
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

    val client = new GenericClient[OrError](
      applicationName = "CogniteScalaSDK-OAuth-Test",
      projectName = "session-testing",
      auth = BearerTokenAuth("bearer Token")
    )(implicitly, implicitly, responseForSessionCreated)

    val resCreate = client.sessions.createWithTokenExchangeFlow()
    resCreate shouldBe Right(expectedResponse)
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
    val responseForSessionCreated = SttpBackendStub(EitherMonad)
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

    val client = new GenericClient[OrError](
      applicationName = "CogniteScalaSDK-OAuth-Test",
      projectName = "session-testing",
      auth = BearerTokenAuth("bearer Token")
    )(implicitly, implicitly, responseForSessionCreated)

    val error = client.sessions
      .createWithClientCredentialFlow(
        Items[SessionCreateWithCredential](
          Seq[SessionCreateWithCredential]()
        )
      ).left.value

    error shouldBe a [CdpApiException]
    inside(error) { case e: CdpApiException =>
      e.message shouldBe s"Request must contain exactly 1 item in request body"
    }
  }

  it should "fail to create a new session with invalid credential" in {
    val expectedError = """
        {
            "error": {
                "code": 403,
                "message": "Resource not found. This may also be due to insufficient access rights."
            }
        }
        """
    val responseForSessionCreated = SttpBackendStub(EitherMonad)
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

    val client = new GenericClient[OrError](
      applicationName = "CogniteScalaSDK-OAuth-Test",
      projectName = "session-testing",
      auth = BearerTokenAuth("bearer Token")
    )(implicitly, implicitly, responseForSessionCreated)

    val error = client.sessions
      .createWithClientCredentialFlow(
        Items[SessionCreateWithCredential](
          Seq(SessionCreateWithCredential("clientId", "clientSecret"))
        )
      )
      .left.value
    
    inside(error) { case e: CdpApiException =>
      e.message shouldBe s"Resource not found. This may also be due to insufficient access rights."
    }
  }

  it should "list all the sessions" in {
    val expectedResponse = Seq(
      SessionList(
        1,
        "CLIENT_CREDENTIALS",
        "READY",
        Instant.now().toEpochMilli,
        Instant.now().plusSeconds(60).toEpochMilli,
        Some("clientId")
      ),
      SessionList(
        2,
        "TOKEN_EXCHANGE",
        "CANCELLED",
        Instant.now().minusSeconds(120).toEpochMilli,
        Instant.now().minusSeconds(60).toEpochMilli
      )
    )
    val responseForSessionList = SttpBackendStub(EitherMonad)
      .whenRequestMatches(r => r.method === Method.GET && r.uri.path.endsWith(List("sessions")))
      .thenRespond(
        Response(
          expectedResponse,
          StatusCode.Ok,
          "OK",
          Seq(Header("content-type", "application/json; charset=utf-8"))
        )
      )

    val client = new GenericClient[OrError](
      applicationName = "CogniteScalaSDK-OAuth-Test",
      projectName = "session-testing",
      auth = BearerTokenAuth("bearer Token")
    )(implicitly, implicitly, responseForSessionList)

    val responseList = client.sessions.list().right.value
    responseList.size shouldBe 2
    responseList shouldBe expectedResponse
  }

  it should "bind a session" in {
    val expectedResponse = SessionTokenResponse(
      1,
      "accessToken",
      Instant.now().toEpochMilli,
      None,
      Some("sessionKey")
    )
    val responseForSessionBound = SttpBackendStub(asyncMonadError[IO])
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

    val client = new GenericClient[IO](
      applicationName = "CogniteScalaSDK-OAuth-Test",
      projectName = "session-testing",
      auth = BearerTokenAuth("bearer Token")
    )(implicitly, implicitly, responseForSessionBound)

    val responseBind = client.sessions.bind(BindSessionRequest("nonce-value")).unsafeRunSync()
    responseBind shouldBe expectedResponse
  }

  it should "fail to bind a session if nonce is expired" in {
    val expectedError = """
        {
            "error": {
                "code": 400,
                "message": "Nonce has expired"
            }
        }
        """
    val responseForSessionBound = SttpBackendStub(asyncMonadError[IO])
      .whenRequestMatches { r =>
        r.method === Method.POST && r.uri.path.endsWith(
          List("sessions", "token")
        ) && r.body === StringBody(
          """{"nonce":"expired-nonce"}""",
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

    val client = new GenericClient[IO](
      applicationName = "CogniteScalaSDK-OAuth-Test",
      projectName = "session-testing",
      auth = BearerTokenAuth("bearer Token")
    )(implicitly, implicitly, responseForSessionBound)

    val error = the[CdpApiException] thrownBy client.sessions
      .bind(BindSessionRequest("expired-nonce"))
      .unsafeRunSync()
    error.message shouldBe "Nonce has expired"
  }

  it should "refresh a session" in {
    val expectedResponse = SessionTokenResponse(
      1,
      "accessToken",
      Instant.now().toEpochMilli,
      None,
      None
    )
    val responseForSessionRefresh = SttpBackendStub(asyncMonadError[IO])
      .whenRequestMatches { r =>
        r.method === Method.POST && r.uri.path.endsWith(
          List("sessions", "token")
        ) && r.body === StringBody(
          """{"sessionId":123,"sessionKey":"sessionKey-value"}""",
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

    val client = new GenericClient[IO](
      applicationName = "CogniteScalaSDK-OAuth-Test",
      projectName = "session-testing",
      auth = BearerTokenAuth("bearer Token")
    )(implicitly, implicitly, responseForSessionRefresh)

    val responseBind =
      client.sessions.refresh(RefreshSessionRequest(123, "sessionKey-value")).unsafeRunSync()
    responseBind shouldBe expectedResponse
  }

  it should "fail to refresh a session with invalid sessionKey" in {
    val expectedError = """
        {
            "error": {
                "code": 400,
                "message": "Session not found"
            }
        }
        """
    val responseForSessionRefresh = SttpBackendStub(asyncMonadError[IO])
      .whenRequestMatches { r =>
        r.method === Method.POST && r.uri.path.endsWith(
          List("sessions", "token")
        ) && r.body === StringBody(
          """{"sessionId":123,"sessionKey":"invalid-sessionKey"}""",
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

    val client = new GenericClient[IO](
      applicationName = "CogniteScalaSDK-OAuth-Test",
      projectName = "session-testing",
      auth = BearerTokenAuth("bearer Token")
    )(implicitly, implicitly, responseForSessionRefresh)

    val error = the[CdpApiException] thrownBy client.sessions
      .refresh(
        RefreshSessionRequest(123, "invalid-sessionKey")
      )
      .unsafeRunSync()
    error.message shouldBe "Session not found"
  }

  it should "revoke a session" in {
    val expectedIds = Seq(
      CogniteInternalId(1),
      CogniteInternalId(2)
    )

    val responseForSessionRevoke = SttpBackendStub(asyncMonadError[IO])
      .whenRequestMatches(r =>
        r.method === Method.POST && r.uri.path.endsWith(List("sessions", "revoke"))
      )
      .thenRespond(
        Response(
          expectedIds,
          StatusCode.Ok,
          "OK",
          Seq(Header("content-type", "application/json; charset=utf-8"))
        )
      )

    val client = new GenericClient[IO](
      applicationName = "CogniteScalaSDK-OAuth-Test",
      projectName = "session-testing",
      auth = BearerTokenAuth("bearer Token")
    )(implicitly, implicitly, responseForSessionRevoke)

    val responseDelete = client.sessions.revoke(Items(expectedIds)).unsafeRunSync()
    responseDelete.size shouldBe 2
    responseDelete shouldBe expectedIds
  }

  implicit val sessionListEncoder: Encoder[SessionList] = deriveEncoder
  it should "parse ids from sessionsList" in {
    val expectedIds = Seq(
      CogniteInternalId(1),
      CogniteInternalId(2)
    )

    val parsed = Seq(
      SessionList(
        1,
        "CLIENT_CREDENTIALS",
        "READY",
        Instant.now().toEpochMilli,
        Instant.now().plusSeconds(60).toEpochMilli,
        Some("clientId")
      ),
      SessionList(
        2,
        "TOKEN_EXCHANGE",
        "CANCELLED",
        Instant.now().minusSeconds(120).toEpochMilli,
        Instant.now().minusSeconds(60).toEpochMilli
      )
    ).asJson
      .as[Seq[CogniteInternalId]]

    parsed shouldBe Right(expectedIds)
  }
}
