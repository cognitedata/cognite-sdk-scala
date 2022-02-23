// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1

import cats.effect.laws.util.TestContext
import cats.effect.{ContextShift, IO, Timer}
import com.cognite.sdk.scala.common.{Items, OAuth2, RetryWhile, SdkTestSpec}
import io.circe.Json
import sttp.client3.asynchttpclient.cats.AsyncHttpClientCatsBackend
import sttp.client3.{SttpBackend, _}

//import cats.Id
//import sttp.client3.testing.SttpBackendStub
//import sttp.model.{Header, MediaType, Method, StatusCode}

import java.util.concurrent.Executors
import scala.collection.immutable.Seq
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt

class DataModelInstancesTest extends SdkTestSpec with RetryWhile {

  val tenant: String = sys.env("TEST_AAD_TENANT_BLUEFIELD")
  val clientId: String = sys.env("TEST_CLIENT_ID_BLUEFIELD")
  val clientSecret: String = sys.env("TEST_CLIENT_SECRET_BLUEFIELD")

  val credentials = OAuth2.ClientCredentials(
    tokenUri = uri"https://login.microsoftonline.com/$tenant/oauth2/v2.0/token",
    clientId = clientId,
    clientSecret = clientSecret,
    scopes = List("https://bluefield.cognitedata.com/.default"),
    cdfProjectName = "extractor-bluefield-testing"
  )

  implicit val testContext: TestContext = TestContext()
  implicit val cs: ContextShift[IO] =
    IO.contextShift(ExecutionContext.fromExecutor(Executors.newFixedThreadPool(4)))
  implicit val timer: Timer[IO] = testContext.timer[IO]

  // Override sttpBackend because this doesn't work with the testing backend
  implicit val sttpBackendAuth: SttpBackend[IO, Any] =
    AsyncHttpClientCatsBackend[IO]().unsafeRunSync()

  val authProvider: OAuth2.ClientCredentialsProvider[IO] =
    OAuth2.ClientCredentialsProvider[IO](credentials).unsafeRunTimed(1.second).get

  val oidcTokenAuth = authProvider.getAuth.unsafeRunSync()

  val bluefieldClient = new GenericClient[IO](
    "scala-sdk-test",
    "extractor-bluefield-testing",
    "https://bluefield.cognitedata.com",
    authProvider,
    None,
    None,
    Some("alpha")
  )

  "DataModelInstances" should "insert data models instances" in {
    val dataModelInstanceToCreate =
      DataModelInstance(
        Some("Equipment-85696cfa"),
        Some("equipment_44"),
        Some(
          Map(
            "name" -> Json.fromString("EQ0001"),
            "col_bool" -> Json.fromBoolean(true),
            "col_float" -> Json.fromDoubleOrNull(1.64)
          )
        )
      )

    val dataModelInstances = bluefieldClient.dataModelInstances
      .createItems(Items[DataModelInstance](Seq(dataModelInstanceToCreate)))
      .unsafeRunSync()
      .toList

    println(s"dataModelInstances = ${dataModelInstances}")
    dataModelInstances.nonEmpty shouldBe true

    // VH TODO remove this once 500 hitting rate is stable
    /*val expectedBody = StringBody(
      s"""{"items":[{"modelExternalId":"${dataModelInstanceToCreate.externalId}","properties":{"name":{"type":"text","nullable":true},"description":{"type":"text","nullable":true}}}]}""".stripMargin,
      "utf-8",
      MediaType.ApplicationJson
    )
    println(s" expectedBody= ${expectedBody}")

    val expectedResponse = Seq(dataModelInstanceToCreate)
    val responseForDataModelCreated = SttpBackendStub.synchronous
      .whenRequestMatches { r =>
        println(s"body is = ${r.body}")
        r.method === Method.POST && r.uri.path.endsWith(
          List("instances", "ingest")
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
      auth = BearerTokenAuth("bearer Token"),
      cdfVersion = Some("alpha")
    )(implicitly, responseForDataModelCreated)

    val resCreate = client.dataModelInstances.createItems(
      Items[DataModelInstance](Seq(dataModelInstanceToCreate))
    )
    resCreate shouldBe expectedResponse*/
  }

  it should "query data models instances" in {}

}
