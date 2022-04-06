// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.common

import java.util.UUID
import cats.Id
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
import cats.effect.unsafe.implicits.global

class LoggingSttpBackend[F[_], +P](delegate: SttpBackend[F, P]) extends SttpBackend[F, P] {
  override def send[T, R >: P with Effect[F]](request: Request[T, R]): F[Response[T]] =
    responseMonad.map(try {
      responseMonad.handleError(delegate.send(request)) {
        case e: Exception =>
          println(s"Exception when sending request: ${request.toString}, ${e.toString}") // scalastyle:ignore
          responseMonad.error(e)
      }
    } catch {
      case NonFatal(e) =>
        println(s"Exception when sending request: ${request.toString}, ${e.toString}") // scalastyle:ignore
        throw e
    }) { response =>
      println(s"request ${request.body.toString}") // scalastyle:ignore
      println(s"response ${response.toString}") // scalastyle:ignore
      if (response.isSuccess) {
        println(s"For request: ${request.toString} got response: ${response.toString}") // scalastyle:ignore
      } else {
        println(s"For request: ${request.toString} got response: ${response.toString}") // scalastyle:ignore
      }
      response
    }
  override def close(): F[Unit] = delegate.close()
  override def responseMonad: MonadError[F] = delegate.responseMonad
}

abstract class SdkTestSpec extends AnyFlatSpec with Matchers with OptionValues {
  implicit val authSttpBackend: SttpBackend[IO, Any] = AsyncHttpClientCatsBackend[IO]().unsafeRunSync()
  // Use this if you need request logs for debugging: new LoggingSttpBackend[Id, Nothing](sttpBackend)
  lazy val client: GenericClient[Id] = GenericClient[Id](
    "scala-sdk-test",
    projectName,
    baseUrl,
    auth
  )

  def shortRandom(): String = UUID.randomUUID().toString.substring(0, 8)
  lazy val projectName: String = "playground"
  lazy val baseUrl: String = GenericClient.defaultBaseUrl
  private lazy val tenant: String = sys.env("TEST_AAD_TENANT")
  private lazy val clientId: String = sys.env("TEST_CLIENT_ID")
  private lazy val clientSecret: String = sys.env("TEST_CLIENT_SECRET")
  private lazy val scopes: List[String] = List(baseUrl + "/.default")

  private lazy val credentials = OAuth2.ClientCredentials(
      tokenUri = uri"https://login.microsoftonline.com/$tenant/oauth2/v2.0/token",
      clientId = clientId,
      clientSecret = clientSecret,
      scopes = scopes,
      cdfProjectName = projectName
    )

  private lazy val authProvider =
      OAuth2.ClientCredentialsProvider[IO](credentials).unsafeRunTimed(1.second).value

  lazy val auth: Auth = authProvider.getAuth.unsafeRunSync()

  lazy val dataSetResource = new DataSets(client.requestSession)

  lazy val testDataSet: DataSet = {
    val list = dataSetResource.filter(DataSetFilter(writeProtected = Some(false))).take(1).compile.toList
    list.headOption.getOrElse({
      dataSetResource.createOne(DataSetCreate(Some("testDataSet"), Some("data set for Scala SDK tests")))
    })
  }
}

abstract class ApiKeysTestSpec extends SdkTestSpec {
  private val apiKey = Option(System.getenv("TEST_API_KEY"))
                  .getOrElse(throw new RuntimeException("TEST_API_KEY not set"))
  override lazy val auth: Auth = ApiKeyAuth(apiKey)

  override lazy val client = GenericClient.forAuth[Id](
    "scala-sdk-test", auth)
}

