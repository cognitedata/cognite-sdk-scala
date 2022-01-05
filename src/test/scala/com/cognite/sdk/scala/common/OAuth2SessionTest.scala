package com.cognite.sdk.scala.common

import cats.effect._
import cats.effect.laws.util.TestContext
import cats.implicits.catsStdInstancesForList
import cats.syntax.parallel._
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

import java.util.concurrent.Executors
import scala.collection.immutable.Seq
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

@SuppressWarnings(Array("org.wartremover.warts.Var"))
class OAuth2SessionTest extends AnyFlatSpec with Matchers with OptionValues {

  implicit val testContext: TestContext = TestContext()
  implicit val cs: ContextShift[IO] =
    IO.contextShift(ExecutionContext.fromExecutor(Executors.newFixedThreadPool(4)))
  implicit val timer: Timer[IO] = testContext.timer[IO]

  // Override sttpBackend because this doesn't work with the testing backend
  implicit val sttpBackend: SttpBackend[IO, Any] = AsyncHttpClientCatsBackend[IO]().unsafeRunSync()

  it should "refresh tokens when they expire" in {
    import sttp.client3.impl.cats.implicits._

    var numTokenRequests = 0

    val session = OAuth2.Session(
      "https://bluefield.cognitedata.com",
      123,
      "sessionKey-value",
      "irrelevant",
      "tokenFromVault"
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
            _ <- IO(numTokenRequests += 1)
            body = SessionTokenResponse(1, "newAccessToken", 5, None, None)

          } yield Response(
            body,
            StatusCode.Ok,
            "OK",
            Seq(Header("content-type", "application/json"))
          )
        }

    val io: IO[Unit] = for {
      authProvider <- OAuth2.SessionProvider[IO](session, refreshSecondsBeforeTTL = 1)
      _ <- List.fill(5)(authProvider.getAuth).parUnorderedSequence
      _ <- IO(numTokenRequests shouldBe 1)
      _ <- IO(testContext.tick(4.seconds))
      _ <- List.fill(5)(authProvider.getAuth).parUnorderedSequence
      _ <- IO(numTokenRequests shouldBe 2)
    } yield ()

    io.unsafeRunTimed(10.seconds).value
  }

  it should "throw a valid error when failing to refresh the session" in {
    val session = OAuth2.Session(
      "https://bluefield.cognitedata.com",
      123,
      "sessionKey-value",
      "irrelevant",
      "tokenFromVault"
    )
    an[CdpApiException] shouldBe thrownBy {
      OAuth2
        .SessionProvider[IO](session)
        .unsafeRunTimed(1.second)
        .value
        .getAuth
        .unsafeRunSync()
    }
  }

  it should "throw an exception when failing to deserialize the refresh response" in {
    val session = OAuth2.Session(
      "https://bluefield.cognitedata.com",
      123,
      "sessionKey-value",
      "irrelevant",
      "tokenFromVault"
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
        .SessionProvider[IO](session)
        .unsafeRunTimed(1.second)
        .value
        .getAuth
        .unsafeRunSync()
    }
  }
}
