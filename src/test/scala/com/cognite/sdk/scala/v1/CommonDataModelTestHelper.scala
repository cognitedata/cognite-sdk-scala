package com.cognite.sdk.scala.v1

import cats.effect.unsafe.implicits.global
import cats.effect.IO
import com.cognite.sdk.scala.common.OAuth2
import com.cognite.sdk.scala.sttp.RetryingBackend
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import sttp.client3._
import sttp.client3.SttpBackend
import sttp.client3.asynchttpclient.cats.AsyncHttpClientCatsBackend

import scala.concurrent.duration.DurationInt

import natchez.Trace

@SuppressWarnings(
  Array(
    "org.wartremover.warts.OptionPartial",
    "org.wartremover.warts.PublicInference"
  )
)
trait CommonDataModelTestHelper extends AnyFlatSpec with Matchers {
  implicit val trace: Trace[IO] = natchez.Trace.Implicits.noop
  val tenant: String = sys.env("TEST_AAD_TENANT")
  val clientId: String = sys.env("TEST_CLIENT_ID")
  val clientSecret: String = sys.env("TEST_CLIENT_SECRET")

  val credentials = OAuth2.ClientCredentials(
    tokenUri = uri"https://login.microsoftonline.com/$tenant/oauth2/v2.0/token",
    clientId = clientId,
    clientSecret = clientSecret,
    scopes = List("https://bluefield.cognitedata.com/.default"),
    cdfProjectName = "extractor-bluefield-testing"
  )

  // Override sttpBackend because this doesn't work with the testing backend
  implicit val sttpBackendAuth: SttpBackend[IO, Any] =
    AsyncHttpClientCatsBackend[IO]().unsafeRunSync()

  val authProvider: OAuth2.ClientCredentialsProvider[IO] =
    OAuth2.ClientCredentialsProvider[IO](credentials).unsafeRunTimed(1.second).get

  lazy val blueFieldClient = new GenericClient[IO](
    "scala-sdk-test",
    "extractor-bluefield-testing",
    "https://bluefield.cognitedata.com",
    authProvider,
    None,
    None,
    Some("alpha")
  )(
    implicitly,
    implicitly,
    new RetryingBackend[IO, Any](implicitly)
  )
}
