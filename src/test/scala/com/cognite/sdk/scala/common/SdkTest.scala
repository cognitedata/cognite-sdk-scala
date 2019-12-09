package com.cognite.sdk.scala.common

import java.util.UUID

import cats.{Comonad, Id, Monad}
import com.cognite.sdk.scala.v1._
import com.softwaremill.sttp.{HttpURLConnectionBackend, MonadError, Request, Response, SttpBackend}
import org.scalatest.{FlatSpec, Matchers}

class LoggingSttpBackend[R[_], S](delegate: SttpBackend[R, S]) extends SttpBackend[R, S] {

  override def send[T](request: Request[T, S]): R[Response[T]] =
    responseMonad.map(responseMonad.handleError(delegate.send(request)) {
      case e: Exception =>
        println(s"Exception when sending request: $request, ${e.toString}") // scalastyle:ignore
        responseMonad.error(e)
    }) { response =>
      println(s"request ${request.body.toString}") // scalastyle:ignore
      println(s"response ${response.toString()}") // scalastyle:ignore
      if (response.isSuccess) {
        println(s"For request: $request got response: $response") // scalastyle:ignore
      } else {
        println(s"For request: $request got response: $response") // scalastyle:ignore
      }
      response
    }
  override def close(): Unit = delegate.close()
  override def responseMonad: MonadError[R] = delegate.responseMonad
}

abstract class SdkTest extends FlatSpec with Matchers {

  val client = new GenericClient[Id, Nothing]("scala-sdk-test")(
    implicitly[Monad[Id]],
    implicitly[Comonad[Id]],
    auth,
    // Use this if you need request logs for debugging: new LoggingSttpBackend[Id, Nothing](sttpBackend)
    new RetryingBackend[Id, Nothing](HttpURLConnectionBackend())
  )

  val greenfieldClient = new GenericClient(
    "cdp-spark-datasource-test", "https://greenfield.cognitedata.com")(
    implicitly[Monad[Id]],
    implicitly[Comonad[Id]],
    greenfieldAuth,
    sttpBackend
  )

  def shortRandom(): String = UUID.randomUUID().toString.substring(0, 8)

  private lazy val apiKey = Option(System.getenv("TEST_API_KEY_READ"))
    .getOrElse(throw new RuntimeException("TEST_API_KEY_READ not set"))
  implicit lazy val auth: Auth = ApiKeyAuth(apiKey)
  private lazy val greenfieldApiKey = Option(System.getenv("TEST_API_KEY_GREENFIELD"))
    .getOrElse(throw new RuntimeException("TEST_API_KEY_GREENFIELD not set"))
  implicit lazy val greenfieldAuth: Auth = ApiKeyAuth(greenfieldApiKey)
}
