// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1

//import cats.Id
import cats.effect.{ContextShift, IO, Timer}
import cats.effect.laws.util.TestContext
import com.cognite.sdk.scala.common.{Items, OAuth2, RetryWhile, SdkTestSpec}
import sttp.client3.SttpBackend
import sttp.client3.asynchttpclient.cats.AsyncHttpClientCatsBackend
import sttp.client3._
//import sttp.client3.testing.SttpBackendStub
//import sttp.model.{Header, MediaType, Method, StatusCode}

import java.util.UUID
import java.util.concurrent.Executors
import scala.collection.immutable.Seq
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt

class DataModelsTest extends SdkTestSpec with RetryWhile {

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

  val blueFieldClient = new GenericClient[IO](
    "scala-sdk-test",
    "extractor-bluefield-testing",
    "https://bluefield.cognitedata.com",
    authProvider,
    None,
    None,
    Some("alpha")
  )

  "DataModels" should "create data models definitions" in {
    val uuid = UUID.randomUUID.toString
    val dataPropName = DataModelProperty("text", Some(true))
    val dataPropDescription = DataModelProperty("text", Some(true))
    // val dataPropIndex = DataModelPropertyIndex(Some("name_descr"), Some(Seq("name", "description")))

    val dataModel = DataModel(
      s"Equipment-${uuid.substring(0, 8)}",
      Some(
        Map(
          "name" -> dataPropName,
          "description" -> dataPropDescription
        )
      ),
      None, // Some(Seq("Asset", "Pump")),
      None // Some(Seq(dataPropIndex))
    )

    val dataModels =
      blueFieldClient.dataModels
        .createItems(Items[DataModel](Seq(dataModel)))
        .unsafeRunSync()
        .toList
    dataModels.contains(dataModel) shouldBe true

    // VH TODO remove the code below and use real test above when create endpoints doesn't return 500 anymore
    /*val expectedBody = StringBody(
      s"""{"items":[{"externalId":"${dataModel.externalId}",
      "properties":{"name":{"type":"text","nullable":true},
      "description":{"type":"text","nullable":true}}}]}""".stripMargin,
      "utf-8",
      MediaType.ApplicationJson
    )
    val expectedResponse = Seq(dataModel)
    val responseForDataModelCreated = SttpBackendStub.synchronous
      .whenRequestMatches { r =>
        r.method === Method.POST && r.uri.path.endsWith(
          List("definitions", "apply")
        ) && (r.body.equals(expectedBody))
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

    val resCreate = client.dataModels.createItems(
      Items[DataModel](Seq(dataModel))
    )
    resCreate shouldBe expectedResponse*/
  }

  it should "delete data models definitions" in {
    // TODO once delete endpoint is deployed
  }

  it should "list all data models definitions" in {
    val dataModels = blueFieldClient.dataModels.list().unsafeRunSync().toList
    dataModels.nonEmpty shouldBe true
  }

}
