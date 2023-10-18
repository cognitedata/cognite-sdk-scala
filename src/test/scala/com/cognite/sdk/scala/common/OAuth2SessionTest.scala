package com.cognite.sdk.scala.common

import cats.effect._
import cats.effect.implicits.commutativeApplicativeForParallelF
import cats.effect.unsafe.implicits._
import cats.implicits.catsStdInstancesForList
import cats.syntax.all._
import com.cognite.sdk.scala.common.OAuth2.TokenState
import com.cognite.sdk.scala.v1.SessionTokenResponse
import org.scalatest.OptionValues
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import sttp.client3._
import sttp.client3.impl.cats.implicits._
import sttp.client3.asynchttpclient.cats.AsyncHttpClientCatsBackend
import sttp.client3.testing.SttpBackendStub
import sttp.model.{Header, MediaType, Method, StatusCode}
import sttp.monad.MonadError

import scala.collection.immutable.Seq
import scala.concurrent.duration._

@SuppressWarnings(Array("org.wartremover.warts.NonUnitStatements", "org.wartremover.warts.Var"))
class OAuth2SessionTest extends AnyFlatSpec with Matchers with OptionValues with RetryWhile {

  it should "not refresh tokens if original token is still valid" in {
    import sttp.client3.impl.cats.implicits._

    val numTokenRequests = Ref[IO].of[Int](0).unsafeRunSync()

    val session = OAuth2.Session(
      "https://bluefield.cognitedata.com",
      123,
      "sessionKey-value",
      "irrelevant"
    )

    implicit val mockSttpBackend: SttpBackendStub[IO, Any] =
      SttpBackendStub(implicitly[MonadError[IO]])
        .whenRequestMatches { req =>
          req.method === Method.POST && req.uri.scheme.contains("https") &&
            req.uri.host.contains("bluefield.cognitedata.com") &&
            req.uri.path.endsWith(
              Seq("api", "v1", "projects", session.cdfProjectName, "sessions", "token")
            ) &&
            req.headers.contains(Header("Authorization", "Bearer tokenFromVault")) &&
            req.body === StringBody(
              """{"sessionId":123,"sessionKey":"sessionKey-value"}""",
              "utf-8",
              MediaType.ApplicationJson
            )
        }
        .thenRespondF {
          for {
            _ <- numTokenRequests.modify(x => (x + 1, x))
            body = SessionTokenResponse(1, "newAccessToken", 5, None, None)
          } yield Response(body, StatusCode.Ok)
        }

    val io = for {
      authProvider <- OAuth2.SessionProvider[IO](
        session,
        refreshSecondsBeforeExpiration = 1,
        Some(IO("kubernetesServiceToken")),
        Some(TokenState("firstToken", Clock[IO].realTime.map(_.toSeconds).unsafeRunSync() + 5)))
      _ <- List.fill(5)(authProvider.getAuth).parUnorderedSequence
      _ <- numTokenRequests.get.map(_ shouldBe 0)
      _ <- IO.sleep(3.seconds)
      _ <- List.fill(5)(authProvider.getAuth).parUnorderedSequence
      _ <- numTokenRequests.get.map(_ shouldBe 0)
    } yield ()

    io.unsafeRunTimed(10.seconds).value
  }

  it should "refresh tokens when they expire" in {
    import sttp.client3.impl.cats.implicits._

    val numTokenRequests = Ref[IO].of[Int](0).unsafeRunSync()

    val session = OAuth2.Session(
      "https://bluefield.cognitedata.com",
      123,
      "sessionKey-value",
      "irrelevant"
    )

    implicit val mockSttpBackend: SttpBackendStub[IO, Nothing] =
      SttpBackendStub(implicitly[MonadError[IO]])
        .whenRequestMatches { req =>
          req.method === Method.POST && req.uri.scheme.contains("https") &&
          req.uri.host.contains("bluefield.cognitedata.com") &&
          req.uri.path.endsWith(
            Seq("api", "v1", "projects", session.cdfProjectName, "sessions", "token")
          ) &&
          req.headers.contains(Header("Authorization", "Bearer kubernetesServiceToken")) &&
          req.body === StringBody(
            """{"sessionId":123,"sessionKey":"sessionKey-value"}""",
            "utf-8",
            MediaType.ApplicationJson
          )
        }
        .thenRespondF {
          for {
            _ <- numTokenRequests.modify(x => (x + 1, x))
            body = SessionTokenResponse(1, "newAccessToken", 5, None, None)
          } yield Response(
            body,
            StatusCode.Ok,
            "OK",
            Seq(Header("content-type", "application/json"))
          )
        }

    val io = for {
      _ <- numTokenRequests.update(_ => 0)
      authProvider <- OAuth2.SessionProvider[IO](
        session,
        refreshSecondsBeforeExpiration = 2,
        Some(IO("kubernetesServiceToken")),
        Some(TokenState("firstToken", Clock[IO].realTime.map(_.toSeconds).unsafeRunSync() + 4 )))
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

  it should "throw a CdpApiException error when failing to refresh the session" in {
    val session = OAuth2.Session(
      "https://bluefield.cognitedata.com",
      123,
      "sessionKey-value",
      "irrelevant"
    )

    implicit val sttpBackend: SttpBackend[IO, Any] = AsyncHttpClientCatsBackend[IO]().unsafeRunSync()

    val cdpApiException = the[CdpApiException] thrownBy {
      OAuth2
        .SessionProvider[IO](session, getToken = Some(IO("kubernetesServiceToken")))
        .unsafeRunTimed(1.second)
        .value
        .getAuth
        .unsafeRunSync()
    }
    cdpApiException.code shouldBe 401
    cdpApiException.message shouldBe "Unauthorized"
  }

  it should "throw an sdk exception when failing to deserialize the refresh response" in {
    val session = OAuth2.Session(
      "https://bluefield.cognitedata.com",
      123,
      "sessionKey-value",
      "irrelevant"
    )

    implicit val mockSttpBackend: SttpBackendStub[IO, Any] =
      SttpBackendStub(implicitly[MonadError[IO]])
        .whenRequestMatches { req =>
          req.method === Method.POST && req.uri.scheme.contains("https") &&
          req.uri.host.contains("bluefield.cognitedata.com") &&
          req.uri.path.endsWith(
            Seq("api", "v1", "projects", session.cdfProjectName, "sessions", "token")
          ) &&
          req.headers.contains(Header("Authorization", "Bearer kubernetesServiceToken")) &&
          req.body === StringBody(
            """{"sessionId":123,"sessionKey":"sessionKey-value"}""",
            "utf-8",
            MediaType.ApplicationJson
          )
        }
        .thenRespond {
          Response(
            """{"justAWeirdJson":"toto"}""",
            StatusCode.Ok,
            "OK",
            Seq(Header("content-type", "application/json"))
          )
        }

    an[SdkException] shouldBe thrownBy {
      OAuth2
        .SessionProvider[IO](session, getToken = Some(IO("kubernetesServiceToken")))
        .unsafeRunTimed(1.second)
        .value
        .getAuth
        .unsafeRunSync()
    }
  }

  it should "throw an sdk exception when failing to get service token" in {
    val session = OAuth2.Session(
      "https://bluefield.cognitedata.com",
      123,
      "sessionKey-value",
      "irrelevant"
    )

    implicit val mockSttpBackend: SttpBackendStub[IO, Any] = SttpBackendStub(implicitly[MonadError[IO]])

    val sdkException = the[SdkException] thrownBy {
      OAuth2
        .SessionProvider[IO](session, getToken = Some(IO.raiseError(new SdkException("Could not get Kubernetes JWT"))))
        .unsafeRunTimed(1.second)
        .value
        .getAuth
        .unsafeRunSync()
    }
    sdkException.getMessage shouldBe "Could not get Kubernetes JWT"
  }
}
