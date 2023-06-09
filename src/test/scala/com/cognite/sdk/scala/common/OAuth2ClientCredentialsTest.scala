package com.cognite.sdk.scala.common

import cats.effect._
import cats.effect.implicits.commutativeApplicativeForParallelF
import cats.effect.unsafe.implicits._
import cats.implicits.catsStdInstancesForList
import cats.syntax.parallel._
import com.cognite.sdk.scala.common.OAuth2.TokenState
import com.cognite.sdk.scala.v1._
import sttp.client3._
import sttp.client3.asynchttpclient.cats.AsyncHttpClientCatsBackend
import sttp.client3.testing.SttpBackendStub
import io.circe.Json
import org.scalatest.OptionValues
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import sttp.model.{Header, Method, StatusCode}
import sttp.monad.MonadError

import scala.collection.immutable.Seq
import scala.concurrent.duration._

@SuppressWarnings(Array("org.wartremover.warts.Var", "org.wartremover.warts.NonUnitStatements"))
class OAuth2ClientCredentialsTest extends AnyFlatSpec with Matchers with OptionValues with RetryWhile {
  val tenant: String = sys.env("TEST_AAD_TENANT")
  val clientId: String = sys.env("TEST_CLIENT_ID")
  val clientSecret: String = sys.env("TEST_CLIENT_SECRET")

  // Override sttpBackend because this doesn't work with the testing backend
  implicit val sttpBackend: SttpBackend[IO, Any] = AsyncHttpClientCatsBackend[IO]().unsafeRunSync()

  it should "authenticate with Azure AD using OAuth2 in bluefield" in {

    val credentials = OAuth2.ClientCredentials(
      tokenUri = uri"https://login.microsoftonline.com/$tenant/oauth2/v2.0/token",
      clientId = clientId,
      clientSecret = clientSecret,
      scopes = List("https://bluefield.cognitedata.com/.default"),
      cdfProjectName = "extractor-bluefield-testing"
    )

    val authProvider =
      OAuth2.ClientCredentialsProvider[IO](credentials).unsafeRunTimed(1.second).value

    val client = new GenericClient(
      applicationName = "CogniteScalaSDK-OAuth-Test",
      projectName = "extractor-bluefield-testing",
      baseUrl = "https://bluefield.cognitedata.com",
      authProvider = authProvider,
      apiVersion = None,
      clientTag = None,
      cdfVersion = None
    )

//    Reenable this when login.status for tokens is fixed
//    val loginStatus = client.login.status().unsafeRunTimed(10.seconds).get
//    assert(loginStatus.loggedIn)

    noException shouldBe thrownBy {
      client.rawDatabases.list().compile.toVector.unsafeRunTimed(10.seconds).value
    }
  }

  // Aize is moving to Azure so we don't have to test their Idp
  // TODO Reactivated the test if we find new credential or remove the test completely
  ignore should "authenticate with Aize using OAuth2" in {

    val credentials = OAuth2.ClientCredentials(
      tokenUri = uri"https://login.aize.io/oauth/token",
      clientId = sys.env("AIZE_CLIENT_ID"),
      clientSecret = sys.env("AIZE_CLIENT_SECRET"),
      cdfProjectName = "aize",
      audience = Some("https://twindata.io/cdf/T101014843")
    )

    val authProvider =
      OAuth2.ClientCredentialsProvider[IO](credentials).unsafeRunTimed(1.second).value

    val client = new GenericClient(
      applicationName = "CogniteScalaSDK-OAuth-Test",
      projectName = "aize",
      baseUrl = "https://api.cognitedata.com",
      authProvider = authProvider,
      apiVersion = None,
      clientTag = None,
      cdfVersion = None
    )

    noException shouldBe thrownBy {
      client.rawDatabases.list().compile.toVector.unsafeRunTimed(10.seconds).value
    }
  }

  it should "throw a valid error when authenticating with bad credentials" in {
    val credentials = OAuth2.ClientCredentials(
      tokenUri = uri"https://login.microsoftonline.com/$tenant/oauth2/v2.0/token",
      clientId = "clientId",
      clientSecret = "clientSecret",
      scopes = List("https://bluefield.cognitedata.com/.default"),
      cdfProjectName = "extractor-bluefield-testing"
    )

    an[SdkException] shouldBe thrownBy {
      OAuth2
        .ClientCredentialsProvider[IO](credentials)
        .unsafeRunTimed(1.second)
        .value
        .getAuth
        .unsafeRunSync()
    }
  }

  it should "refresh tokens when they expire" in {
    import sttp.client3.impl.cats.implicits._

    val numTokenRequests = Ref[IO].of[Int](0).unsafeRunSync()

    implicit val mockSttpBackend: SttpBackendStub[IO, Any] =
      SttpBackendStub(implicitly[MonadError[IO]])
        .whenRequestMatches(req => req.method === Method.POST && req.uri.path === Seq("token"))
        .thenRespondF {
          for {
            _ <- numTokenRequests.modify(x => (x + 1, x))
            body = Json.obj(
              "access_token" -> Json.fromString("foo"),
              "expires_in" -> Json.fromString("5")
            )
          } yield Response(
            body.noSpaces,
            StatusCode.Ok,
            "OK",
            Seq(Header("content-type", "application/json"))
          )
        }

    val credentials = OAuth2.ClientCredentials(
      tokenUri = uri"http://whatever.com/token",
      clientId = "irrelevant",
      clientSecret = "irrelevant",
      scopes = List("irrelevant"),
      cdfProjectName = "irrelevant"
    )

    val io = for {
      _ <- numTokenRequests.update(_ => 0)
      authProvider <- OAuth2.ClientCredentialsProvider[IO](credentials,
        refreshSecondsBeforeExpiration = 2,
        Some(IO.pure(Some(TokenState("firstToken", Clock[IO].realTime.map(_.toSeconds).unsafeRunSync() + 4, "irrelevant")))))
      _ <- List.fill(5)(authProvider.getAuth).parUnorderedSequence
      noNewToken <- numTokenRequests.get  // original token is still valid
      _ <- IO.sleep(4.seconds)
      _ <- List.fill(5)(authProvider.getAuth).parUnorderedSequence
      oneRequestedToken <- numTokenRequests.get // original token is expired
      _ <- IO.sleep(4.seconds)
      _ <- List.fill(5)(authProvider.getAuth).parUnorderedSequence
      twoRequestedToken <- numTokenRequests.get // first renew token is expired
    } yield (noNewToken, oneRequestedToken, twoRequestedToken)

    retryWithExpectedResult[(Int,Int,Int)](
      io.unsafeRunTimed(10.seconds).value,
      r => r shouldBe ((0, 1, 2))
    )
  }
}
