// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1

import cats.effect.laws.util.TestContext
import cats.effect.{ContextShift, IO, Timer}
import com.cognite.sdk.scala.common.{OAuth2, RetryWhile, SdkTestSpec}
import sttp.client3.{SttpBackend, _}
import sttp.client3.asynchttpclient.cats.AsyncHttpClientCatsBackend

import java.util.concurrent.Executors
import scala.collection.immutable.Seq
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt

class DataModelMappingsTest extends SdkTestSpec with RetryWhile {

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

  "DataModelMappings" should "list all data models mappings" in {
    val dataModelMappings = bluefieldClient.dataModelMappings.list().unsafeRunSync().toList
    dataModelMappings.nonEmpty shouldBe true
  }

  it should "get data models mappings by externalIds" in {
    val dataModelMappings =
      bluefieldClient.dataModelMappings
        .retrieveByExternalIds(
          Seq("Equipment-996bb3e4", "Equipment-9fdb2ad4")
        )
        .unsafeRunSync()
        .toList
    dataModelMappings.size shouldBe 2
  }

}
