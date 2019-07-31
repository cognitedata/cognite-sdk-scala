package com.cognite.sdk.scala.common

import java.util.UUID

import cats.{Id, Monad}
import com.cognite.sdk.scala.v1.{GenericClient, sttpBackend}
import com.softwaremill.sttp.{MonadError, Request, Response, SttpBackend}
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
  val client = new GenericClient("scala-sdk-test")(implicitly[Monad[Id]], auth, sttpBackend)

  def shortRandom(): String = UUID.randomUUID().toString.substring(0, 8)

  private lazy val apiKey = Option(System.getenv("TEST_API_KEY_READ"))
    .getOrElse(throw new RuntimeException("TEST_API_KEY_READ not set"))
  implicit lazy val auth: Auth = ApiKeyAuth(apiKey)
}
