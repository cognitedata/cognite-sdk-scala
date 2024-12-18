// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.common

import java.util.UUID
import com.cognite.sdk.scala.sttp.RetryingBackend
import com.cognite.sdk.scala.v1._
import com.cognite.sdk.scala.v1.resources.DataSets
import sttp.client3._
import sttp.client3.asynchttpclient.cats.AsyncHttpClientCatsBackend
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.OptionValues
import sttp.capabilities.Effect
import sttp.monad.MonadError

import scala.concurrent.duration._
import scala.util.control.NonFatal
import cats.effect.IO
import cats.effect.unsafe.IORuntime

import natchez.Trace

class LoggingSttpBackend[F[_], +P](delegate: SttpBackend[F, P]) extends SttpBackend[F, P] {
  override def send[T, R >: P with Effect[F]](request: Request[T, R]): F[Response[T]] =
    responseMonad.map(try {
      responseMonad.handleError(delegate.send(request)) {
        case e: Exception =>
          println(s"Exception when sending request: ${request.toString}, ${e.toString}")
          responseMonad.error(e)
      }
    } catch {
      case NonFatal(e) =>
        println(s"Exception when sending request: ${request.toString}, ${e.toString}")
        throw e
    }) { response =>
      println(s"request ${request.body.toString}")
      println(s"response ${response.toString}")
      if (response.isSuccess) {
        println(s"For request: ${request.toString} got response: ${response.toString}")
      } else {
        println(s"For request: ${request.toString} got response: ${response.toString}")
      }
      response
    }
  override def close(): F[Unit] = delegate.close()
  override def responseMonad: MonadError[F] = delegate.responseMonad
}

abstract class SdkTestSpec extends AnyFlatSpec with Matchers with OptionValues {
  implicit val ioRuntime: IORuntime = IORuntime.global
  implicit val trace: Trace[IO] = natchez.Trace.Implicits.noop
  implicit val traceEither: Trace[OrError] = natchez.Trace.Implicits.noop
  implicit val authSttpBackend: SttpBackend[IO, Any] = AsyncHttpClientCatsBackend[IO]().unsafeRunSync()
  // Use this if you need request logs for debugging: new LoggingSttpBackend[Id, Nothing](sttpBackend)
  lazy val client: GenericClient[IO] = GenericClient[IO](
    "scala-sdk-test",
    projectName,
    baseUrl,
    auth
  )(
    implicitly,
    implicitly,
    new RetryingBackend[IO, Any](AsyncHttpClientCatsBackend[IO]().unsafeRunSync())
  )

  def shortRandom(): String = UUID.randomUUID().toString.substring(0, 8)
  lazy val projectName: String = sys.env.getOrElse("TEST_PROJECT2", "playground")
  lazy val baseUrl: String = sys.env.getOrElse("COGNITE_BASE_URL2", GenericClient.defaultBaseUrl)
  lazy val audience = Some(baseUrl)
  lazy val tokenUri = sys.env.get("TEST_TOKEN_URL2")
    .orElse(
      sys.env.get("TEST_AAD_TENANT2")
        .map(tenant => s"https://login.microsoftonline.com/$tenant/oauth2/v2.0/token"))
    .getOrElse("https://sometokenurl")
  lazy val clientId: String = sys.env("TEST_CLIENT_ID2")
  lazy val clientSecret: String = sys.env("TEST_CLIENT_SECRET2")
  lazy val scopes: List[String] = List(baseUrl + "/.default")

  lazy val credentials = OAuth2.ClientCredentials(
      tokenUri = uri"${tokenUri}",
      clientId = clientId,
      clientSecret = clientSecret,
      scopes = scopes,
      audience = audience,
      cdfProjectName = projectName
    )

  private lazy val authProvider =
      OAuth2.ClientCredentialsProvider[IO](credentials).unsafeRunTimed(1.second).value

  lazy val auth: Auth = authProvider.getAuth.unsafeRunSync()

  lazy val dataSetResource = new DataSets(client.requestSession)

  lazy val testDataSet: DataSet = {
    val list = dataSetResource.filter(DataSetFilter(writeProtected = Some(false))).take(1).compile.toList.unsafeRunSync()
    list.headOption.getOrElse({
      dataSetResource.createOne(DataSetCreate(Some("testDataSet"), Some("data set for Scala SDK tests"))).unsafeRunSync()
    })
  }
}

