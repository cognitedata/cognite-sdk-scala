// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.common

import java.util.UUID
import cats.Id
import cats.catsInstancesForId
import com.cognite.sdk.scala.v1._
import com.cognite.sdk.scala.v1.resources.DataSets
import sttp.client3.{Request, Response, SttpBackend}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import sttp.capabilities.Effect
import sttp.monad.MonadError

import scala.util.control.NonFatal

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

abstract class SdkTestSpec extends AnyFlatSpec with Matchers {
  // Use this if you need request logs for debugging: new LoggingSttpBackend[Id, Nothing](sttpBackend)
  lazy val client: GenericClient[Id] = GenericClient.forAuth[Id](
    "scala-sdk-test", auth)(implicitly, sttpBackend)

  lazy val greenfieldClient: GenericClient[Id] = GenericClient.forAuth[Id](
    "scala-sdk-test", greenfieldAuth, "https://greenfield.cognitedata.com")(implicitly, sttpBackend)

  lazy val projectName: String = client.login.status().project

  def shortRandom(): String = UUID.randomUUID().toString.substring(0, 8)

  private lazy val apiKey = Option(System.getenv("TEST_API_KEY"))
    .getOrElse(throw new RuntimeException("TEST_API_KEY not set"))
  implicit lazy val auth: Auth = ApiKeyAuth(apiKey)
  private lazy val greenfieldApiKey = Option(System.getenv("TEST_API_KEY_GREENFIELD"))
    .getOrElse(throw new RuntimeException("TEST_API_KEY_GREENFIELD not set"))
  implicit lazy val greenfieldAuth: Auth = ApiKeyAuth(greenfieldApiKey)

  lazy val dataSetResource = new DataSets(client.requestSession)

  lazy val testDataSet = {
    val list = dataSetResource.filter(DataSetFilter(writeProtected = Some(false))).take(1).compile.toList
    list.headOption.getOrElse({
      dataSetResource.createOne(DataSetCreate(Some("testDataSet"), Some("data set for Scala SDK tests")))
    })
  }
}
