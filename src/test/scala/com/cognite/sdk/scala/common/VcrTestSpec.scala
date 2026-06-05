// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.common

import cats.effect.IO
import cats.effect.unsafe.IORuntime
import com.cognite.sdk.scala.sttp.{RetryingBackend, VcrBackend, VcrMode}
import com.cognite.sdk.scala.v1._
import natchez.Trace
import org.scalatest.{BeforeAndAfterEachTestData, TestData}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.OptionValues
import sttp.client3._
import sttp.client3.asynchttpclient.cats.AsyncHttpClientCatsBackend
import sttp.client3.impl.cats.CatsMonadAsyncError
import sttp.client3.testing.SttpBackendStub

import scala.concurrent.duration.DurationInt

/** Base class for tests that use VCR (cassette-based HTTP recording/playback).
  *
  * In RECORD mode (VCR_MODE=RECORD or AUTO when no cassette exists): tests run against the real
  * Cognite API and all HTTP interactions are saved to cassette files under
  * src/test/resources/cassettes/{ClassName}/{testName}.json.
  *
  * In PLAYBACK mode (VCR_MODE=PLAYBACK or AUTO when cassette exists): tests replay recorded
  * interactions without making real HTTP calls. No credentials are required.
  *
  * The mode is controlled by the VCR_MODE environment variable. Default is AUTO.
  *
  * Override `cdfVersion` to add the `cdf-version` request header (e.g. `Some("alpha")`).
  */
@SuppressWarnings(Array("org.wartremover.warts.Var", "org.wartremover.warts.Equals"))
abstract class VcrTestSpec
    extends AnyFlatSpec
    with Matchers
    with OptionValues
    with BeforeAndAfterEachTestData {

  implicit val ioRuntime: IORuntime = IORuntime.global
  implicit val trace: Trace[IO] = natchez.Trace.Implicits.noop

  private var _client: Option[GenericClient[IO]] = None
  private var _rawClient: Option[GenericClient[IO]] = None
  private var _vcrBackend: Option[VcrBackend[IO]] = None

  /** Client with `RetryingBackend`. Use this for normal operations. */
  def client: GenericClient[IO] =
    _client.getOrElse(sys.error("VCR client not initialized — called outside of a test?"))

  /** Client without retrying. Use this when testing error responses directly. */
  def rawClient: GenericClient[IO] =
    _rawClient.getOrElse(sys.error("VCR rawClient not initialized — called outside of a test?"))

  /** Suffix appended to credential env var names: TEST_CLIENT_ID{suffix}, TEST_AAD_TENANT{suffix}, etc.
    * Override to `""` to use the un-suffixed vars (TEST_CLIENT_ID, COGNITE_BASE_URL, …).
    */
  protected def envVarSuffix: String = "2"

  def projectName: String = sys.env.getOrElse(s"TEST_PROJECT$envVarSuffix", "playground")
  def baseUrl: String = sys.env.getOrElse(s"COGNITE_BASE_URL$envVarSuffix", GenericClient.defaultBaseUrl)
  def cassettesBaseDir: String = "src/test/resources/cassettes"

  /** Override to add the `cdf-version` header to all requests (e.g. `Some("alpha")`). */
  protected def cdfVersion: Option[String] = None

  def cassettePath(testName: String): String = {
    val className = getClass.getSimpleName
    val safeTestName = simplifyFilename(testName)
    s"$cassettesBaseDir/$className/$safeTestName.json"
  }

  override def beforeEach(testData: TestData): Unit = {
    val path = cassettePath(testData.name)
    val mode = VcrMode.fromEnv(VcrMode.Auto) match {
      case VcrMode.Auto =>
        if (java.nio.file.Files.exists(java.nio.file.Paths.get(path))) VcrMode.Playback
        else VcrMode.Record
      case other => other
    }
    mode match {
      case VcrMode.Playback =>
        val dummyBackend: SttpBackend[IO, Any] =
          SttpBackendStub[IO, Any](new CatsMonadAsyncError[IO]())
            .whenAnyRequest
            .thenRespondServerError()
        val vcr = new VcrBackend[IO](dummyBackend, path, VcrMode.Playback)
        _vcrBackend = Some(vcr)
        val fakeAuth = BearerTokenAuth("vcr-playback-token")
        // During playback retrying doesn't affect cassette matching, use identity for both
        _client = Some(buildClient(vcr, fakeAuth, identity))
        _rawClient = Some(buildClient(vcr, fakeAuth, identity))

      case _ =>
        val auth = fetchRealAuth()
        val realHttp = AsyncHttpClientCatsBackend[IO]().unsafeRunSync()
        // VCR wraps realHttp directly so every individual HTTP call (including retries) is recorded
        val vcr = new VcrBackend[IO](realHttp, path, mode)
        _vcrBackend = Some(vcr)
        _client = Some(buildClient(vcr, auth, new RetryingBackend[IO, Any](_)))
        _rawClient = Some(buildClient(vcr, auth, identity))
    }
  }

  override def afterEach(testData: TestData): Unit = {
    _vcrBackend.foreach(_.close().unsafeRunSync())
    _vcrBackend = None
    _client = None
    _rawClient = None
  }

  private def fetchRealAuth(): Auth = {
    val s = envVarSuffix
    val tokenUri = sys.env
      .get(s"TEST_TOKEN_URL$s")
      .orElse(
        sys.env
          .get(s"TEST_AAD_TENANT$s")
          .map(t => s"https://login.microsoftonline.com/$t/oauth2/v2.0/token")
      )
      .getOrElse(
        sys.error(s"TEST_TOKEN_URL$s or TEST_AAD_TENANT$s must be set for VCR RECORD mode")
      )
    val clientId =
      sys.env.getOrElse(s"TEST_CLIENT_ID$s", sys.error(s"TEST_CLIENT_ID$s must be set"))
    val clientSecret =
      sys.env.getOrElse(s"TEST_CLIENT_SECRET$s", sys.error(s"TEST_CLIENT_SECRET$s must be set"))

    val credentials = OAuth2.ClientCredentials(
      tokenUri = uri"${tokenUri}",
      clientId = clientId,
      clientSecret = clientSecret,
      scopes = List(s"$baseUrl/.default"),
      audience = Some(baseUrl),
      cdfProjectName = projectName
    )

    implicit val authBackend: SttpBackend[IO, Any] =
      AsyncHttpClientCatsBackend[IO]().unsafeRunSync()
    try {
      val authProvider =
        OAuth2.ClientCredentialsProvider[IO](credentials).unsafeRunTimed(10.seconds).value
      authProvider.getAuth.unsafeRunSync()
    } finally {
      authBackend.close().unsafeRunSync()
    }
  }

  private def buildClient(
      backend: SttpBackend[IO, Any],
      auth: Auth,
      wrap: SttpBackend[IO, Any] => SttpBackend[IO, Any]
  ): GenericClient[IO] =
    new GenericClient[IO](
      applicationName = "scala-sdk-vcr-test",
      projectName = projectName,
      baseUrl = baseUrl,
      auth = auth,
      cdfVersion = cdfVersion,
      sttpBackend = backend,
      wrapSttpBackend = wrap
    )

  def shortRandom(): String = java.util.UUID.randomUUID().toString.substring(0, 8)

  private def simplifyFilename(name: String): String = {
    val initial = name.replaceAll("[^a-zA-Z0-9._]", "-")

    @scala.annotation.tailrec
    def loop(s: String): String = {
      val next = s
        .replace("--", "-")
        .replace(".-", ".")
        .replace("-.", ".")
        .replace("..", ".")
      if (next.equals(s)) s else loop(next)
    }

    loop(initial).stripPrefix("-").stripSuffix("-")
  }
}
