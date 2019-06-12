package com.cognite.sdk.scala.common

import com.softwaremill.sttp.{HttpURLConnectionBackend, Id, MonadError, Request, Response, SttpBackend, SttpBackendOptions}
import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.duration._

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
  private val apiKey = Option(System.getenv("TEST_API_KEY_READ"))
    .getOrElse(throw new RuntimeException("TEST_API_KEY_READ not set"))
  //implicit val backend: SttpBackend[Id, Nothing] = new LoggingSttpBackend[Id, Nothing](HttpURLConnectionBackend())
  implicit val backend: SttpBackend[Id, Nothing] = HttpURLConnectionBackend(
    options = SttpBackendOptions.connectionTimeout(90.seconds)
  )
  implicit val auth: Auth = ApiKeyAuth(apiKey)
}
