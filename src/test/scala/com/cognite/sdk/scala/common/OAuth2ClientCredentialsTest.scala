package com.cognite.sdk.scala.common

import java.util.concurrent.Executors

import cats.effect._
import cats.effect.laws.util.TestContext
import cats.implicits.catsStdInstancesForList
import cats.syntax.all._
import com.cognite.sdk.scala.v1._
import com.softwaremill.sttp._
import com.softwaremill.sttp.asynchttpclient.cats.AsyncHttpClientCatsBackend
import com.softwaremill.sttp.testing.SttpBackendStub
import io.circe.Json
import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

class OAuth2ClientCredentialsTest extends FlatSpec with Matchers {
  val tenant = sys.env("TEST_AAD_TENANT_BLUEFIELD")
  val clientId = sys.env("TEST_CLIENT_ID_BLUEFIELD")
  val clientSecret = sys.env("TEST_CLIENT_SECRET_BLUEFIELD")

  implicit val testContext: TestContext = TestContext()
  implicit val cs: ContextShift[IO] = IO.contextShift(ExecutionContext.fromExecutor(Executors.newFixedThreadPool(1)))
  implicit val timer: Timer[IO] = testContext.timer[IO]


  it should "authenticate with Azure AD using OAuth2 in bluefield" in {
    // Override sttpBackend because this doesn't work with the testing backend
    implicit val sttpBackend: SttpBackend[IO, Nothing] = AsyncHttpClientCatsBackend[IO]()

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

    // Reenable this when login.status for tokensis fixed
//    val loginStatus = client.login.status().unsafeRunTimed(10.seconds).get
//    assert(loginStatus.loggedIn)

    noException shouldBe thrownBy {
      client.rawDatabases.list().compile.toVector.unsafeRunTimed(10.seconds).get
    }
  }

  it should "refresh tokens when they expire" in {
    import com.softwaremill.sttp.impl.cats.implicits._

    var numTokenRequests = 0

    implicit val mockSttpBackend = SttpBackendStub(implicitly[MonadError[IO]])
      .whenRequestMatches(req => req.method == Method.POST && req.uri.path == Seq("token"))
      .thenRespondWrapped {
        for {
          _ <- IO { numTokenRequests += 1 }
          body = Json.obj(
            "access_token" -> Json.fromString("foo"),
            "expires_in" -> Json.fromString("2")
          )
        } yield Response(Right(body.noSpaces), 200, "OK", List("content-type" -> "application/json"), Nil)
      }

    val credentials = OAuth2.ClientCredentials(
      tokenUri = uri"http://whatever.com/token",
      clientId = "irrelevant",
      clientSecret = "irrelevant",
      scopes = List("irrelevant")
    )

    val io: IO[Unit] = for {
      authProvider <- OAuth2.ClientCredentialsProvider[IO](credentials, refreshSecondsBeforeTTL = 1)
      _ <- List.fill(5)(authProvider.getAuth).parUnorderedSequence
      _ <- IO { numTokenRequests shouldBe 1 }
      _ <- IO { testContext.tick(1.seconds) }
      _ <- List.fill(5)(authProvider.getAuth).parUnorderedSequence
      _ <- IO { numTokenRequests shouldBe 2 }
    } yield ()

    io.unsafeRunTimed(10.seconds).get
  }
}
