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
  println(s" oidcTokenAuth= ${oidcTokenAuth}")

  val bluefieldClient = new GenericClient[IO](
    "scala-sdk-test",
    "extractor-bluefield-testing",
    "https://bluefield.cognitedata.com",
    authProvider,
    None,
    None,
    Some("alpha")
  )

  val dataModelInstanceToCreate1 =
    DataModelInstance(
      Some("Equipment-85696cfa"),
      Some("equipment_43"),
      Some(
        Map(
          "name" -> Json.fromString("EQ0001"),
          "col_float" -> Json.fromDoubleOrNull(0)
        )
      )
    )

  val dataModelInstanceToCreate2 =
    DataModelInstance(
      Some("Equipment-85696cfa"),
      Some("equipment_44"),
      Some(
        Map(
          "name" -> Json.fromString("EQ0002"),
          "col_bool" -> Json.fromBoolean(true),
          "col_float" -> Json.fromDoubleOrNull(1.64)
        )
      )
    )

  val dataModelInstanceToCreate3 =
    DataModelInstance(
      Some("Equipment-85696cfa"),
      Some("equipment_45"),
      Some(
        Map(
          "name" -> Json.fromString("EQ0011"),
          "col_bool" -> Json.fromBoolean(false),
          "col_float" -> Json.fromDoubleOrNull(3.5)
        )
      )
    )

  val toCreates =
    Seq(dataModelInstanceToCreate1, dataModelInstanceToCreate2, dataModelInstanceToCreate3)

  "Insert data model instances" should "work with multiple input" in {
    val dataModelInstances = bluefieldClient.dataModelInstances
      .createItems(
        Items[DataModelInstance](
          toCreates
        )
      )
      .unsafeRunSync()
      .toList

    dataModelInstances.size shouldBe 3
    dataModelInstances.map(_.properties).toSet shouldBe toCreates.map(_.properties).toSet

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

  "Query data model instances" should "work with empty filter" in {
    val inputNoFilterQuery = DataModelInstanceQuery("Equipment-85696cfa")
    val outputNoFilter = bluefieldClient.dataModelInstances
      .query(inputNoFilterQuery)
      .unsafeRunSync()
    println(s"outputNoFilter = ${outputNoFilter}")
    outputNoFilter.items.toList.size shouldBe 3
  }

  it should "work with AND filter" in {
    val inputQueryAnd = DataModelInstanceQuery(
      "Equipment-85696cfa",
      Some(
        DMIAndFilter(
          Seq(
            DMIEqualsFilter(Seq("name"), Json.fromString("EQ0002")),
            DMIEqualsFilter(Seq("col_bool"), Json.fromBoolean(true)),
            DMIEqualsFilter(Seq("col_float"), Json.fromFloatOrNull(1.64f))
          )
        )
      )
    )
    val outputQueryAnd = bluefieldClient.dataModelInstances
      .query(inputQueryAnd)
      .unsafeRunSync()
      .items
      .toList

    outputQueryAnd.size shouldBe 1
    outputQueryAnd.map(_.properties).toSet shouldBe Set(dataModelInstanceToCreate2.properties)

    val inputQueryAnd2 = DataModelInstanceQuery(
      "Equipment-85696cfa",
      Some(
        DMIAndFilter(
          Seq(
            DMIEqualsFilter(Seq("name"), Json.fromString("EQ0001")),
            DMIEqualsFilter(Seq("col_bool"), Json.fromBoolean(true))
          )
        )
      )
    )
    val outputQueryAndEmpty = bluefieldClient.dataModelInstances
      .query(inputQueryAnd2)
      .unsafeRunSync()
      .items
      .toList

    outputQueryAndEmpty.isEmpty shouldBe true
  }

  it should "work with OR filter" in {
    val inputQueryOr = DataModelInstanceQuery(
      "Equipment-85696cfa",
      Some(
        DMIOrFilter(
          Seq(
            DMIEqualsFilter(Seq("name"), Json.fromString("EQ0011")),
            DMIEqualsFilter(Seq("col_bool"), Json.fromBoolean(true))
          )
        )
      )
    )
    val outputQueryOr = bluefieldClient.dataModelInstances
      .query(inputQueryOr)
      .unsafeRunSync()
      .items
      .toList

    outputQueryOr.size shouldBe 2
    outputQueryOr.map(_.properties).toSet shouldBe Set(
      dataModelInstanceToCreate2.properties,
      dataModelInstanceToCreate3.properties
    )
  }

  it should "work with NOT filter" in {
    val inputQueryNot = DataModelInstanceQuery(
      "Equipment-85696cfa",
      Some(
        DMINotFilter(
          DMIInFilter(Seq("name"), Seq(Json.fromString("EQ0002"), Json.fromString("EQ0011")))
        )
      )
    )
    val outputQueryNot = bluefieldClient.dataModelInstances
      .query(inputQueryNot)
      .unsafeRunSync()
      .items
      .toList

    println(s"outputQueryNot = ${outputQueryNot}")
    outputQueryNot.size shouldBe 1
    outputQueryNot.map(_.properties).toSet shouldBe Set(dataModelInstanceToCreate1.properties)
  }

  it should "work with PREFIX filter" in {
    val inputQueryPrefix = DataModelInstanceQuery(
      "Equipment-85696cfa",
      Some(
        DMIPrefixFilter(Seq("name"), Json.fromString("EQ000"))
      )
    )
    val outputQueryPrefix = bluefieldClient.dataModelInstances
      .query(inputQueryPrefix)
      .unsafeRunSync()
      .items
      .toList

    outputQueryPrefix.size shouldBe 2
    outputQueryPrefix.map(_.properties).toSet shouldBe Set(
      dataModelInstanceToCreate1.properties,
      dataModelInstanceToCreate2.properties
    )
  }

  it should "work with RANGE filter" in {
    val inputQueryRange = DataModelInstanceQuery(
      "Equipment-85696cfa",
      Some(
        DMIRangeFilter(Seq("col_float"), gte = Some(Json.fromFloatOrNull(1.64f)))
      )
    )
    val outputQueryRange = bluefieldClient.dataModelInstances
      .query(inputQueryRange)
      .unsafeRunSync()
      .items
      .toList

    outputQueryRange.map(_.properties).toSet shouldBe Set(
      dataModelInstanceToCreate2.properties,
      dataModelInstanceToCreate3.properties
    )
  }
}
