package com.cognite.sdk.scala.common

import cats.effect._
import com.cognite.sdk.scala.v1._
import com.softwaremill.sttp._
import com.softwaremill.sttp.asynchttpclient.cats.AsyncHttpClientCatsBackend
import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

class OAuth2ClientCredentialsTest extends FlatSpec with Matchers {
  ignore should "authenticate using OAuth2" in { // TODO: Add credentials
    val ec: ExecutionContext = ExecutionContext.global
    implicit val cs: ContextShift[IO] = IO.contextShift(ec)
    implicit val timer: Timer[IO] = IO.timer(ec)
    implicit val sttpBackend: SttpBackend[IO, Nothing] = AsyncHttpClientCatsBackend[IO]()

    val tenant = sys.env("TEST_TOKEN_URL")
    val clientId = sys.env("TEST_OAUTH2_CLIENT_ID_BLUEFIELD")
    val clientSecret = sys.env("TEST_OAUTH2_CLIENT_SECRET_BLUEFIELD")

    val credentials = OAuth2.ClientCredentials(
      tokenUri = uri"https://login.microsoftonline.com/$tenant/oauth2/v2.0/token",
      clientId = clientId,
      clientSecret = clientSecret,
      scopes = List("https://bluefield.cognitedata.com/.default")
    )

    val authProvider = OAuth2.ClientCredentialsProvider[IO](credentials).unsafeRunTimed(1.second).get

    val client = new GenericClient(
      applicationName = "CogniteScalaSDK-OAuth-Test",
      projectName = "extractor-bluefield-testing",
      baseUrl = "https://bluefield.cognitedata.com",
      authProvider = authProvider,
      apiVersion = None,
      clientTag = None
    )

    val loginStatus = client.login.status().unsafeRunTimed(10.seconds).get
    assert(loginStatus.loggedIn)

    noException shouldBe thrownBy {
      client.rawDatabases.list().compile.toVector.unsafeRunTimed(10.seconds)
    }
  }
}
