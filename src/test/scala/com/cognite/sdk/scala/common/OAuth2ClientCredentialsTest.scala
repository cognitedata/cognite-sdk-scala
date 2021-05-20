package com.cognite.sdk.scala.common

import java.util.concurrent.Executors
import cats.effect._
import cats.effect.laws.util.TestContext
import cats.implicits.catsStdInstancesForList
import cats.syntax.all._
import com.cognite.sdk.scala.v1._
import sttp.client3._
import sttp.client3.asynchttpclient.cats.AsyncHttpClientCatsBackend
import sttp.client3.testing.SttpBackendStub
import io.circe.Json
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import sttp.model.{Header, Method, StatusCode}
import sttp.monad.MonadError

import scala.collection.immutable.Seq
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

class OAuth2ClientCredentialsTest extends AnyFlatSpec with Matchers {
  val tenant: String = sys.env("TEST_AAD_TENANT_BLUEFIELD")
  val clientId: String = sys.env("TEST_CLIENT_ID_BLUEFIELD")
  val clientSecret: String = sys.env("TEST_CLIENT_SECRET_BLUEFIELD")

  implicit val testContext: TestContext = TestContext()
  implicit val cs: ContextShift[IO] = IO.contextShift(ExecutionContext.fromExecutor(Executors.newFixedThreadPool(4)))
  implicit val timer: Timer[IO] = testContext.timer[IO]


  // Override sttpBackend because this doesn't work with the testing backend
  implicit val sttpBackend: SttpBackend[IO, Any] = AsyncHttpClientCatsBackend[IO]().unsafeRunSync()

  it should "authenticate with Azure AD using OAuth2 in bluefield" in {

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

//    Reenable this when login.status for tokens is fixed
//    val loginStatus = client.login.status().unsafeRunTimed(10.seconds).get
//    assert(loginStatus.loggedIn)

    noException shouldBe thrownBy {
      client.rawDatabases.list().compile.toVector.unsafeRunTimed(10.seconds).get
    }
  }

  it should "throw a valid error when authenticating with bad credentials" in {
    val credentials = OAuth2.ClientCredentials(
      tokenUri = uri"https://login.microsoftonline.com/$tenant/oauth2/v2.0/token",
      clientId = "clientId",
      clientSecret = "clientSecret",
      scopes = List("https://bluefield.cognitedata.com/.default")
    )

    an[SdkException] shouldBe thrownBy {
      OAuth2.ClientCredentialsProvider[IO](credentials).unsafeRunTimed(1.second).get.getAuth.unsafeRunSync()
    }
  }

  it should "refresh tokens when they expire" in {
    import sttp.client3.impl.cats.implicits._

    var numTokenRequests = 0

    implicit val mockSttpBackend: SttpBackendStub[IO, Any] = SttpBackendStub(implicitly[MonadError[IO]])
      .whenRequestMatches(req => req.method == Method.POST && req.uri.path == Seq("token"))
      .thenRespondF {
        for {
          _ <- IO { numTokenRequests += 1 }
          body = Json.obj(
            "access_token" -> Json.fromString("foo"),
            "expires_in" -> Json.fromString("2")
          )
        } yield Response(body.noSpaces, StatusCode.Ok, "OK", Seq(Header("content-type", "application/json")))
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
