package com.cognite.sdk.scala.sttp

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import sttp.client3._
import sttp.monad.MonadError
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import sttp.capabilities.Effect
import sttp.client3.asynchttpclient.cats.AsyncHttpClientCatsBackend

import java.net.UnknownHostException
import scala.concurrent.duration.DurationInt

@SuppressWarnings(Array("org.wartremover.warts.Var"))
class FailingRetryingBackend[F[_], +P](delegate: SttpBackend[F, P]) extends SttpBackend[F, P] {
  var callCount: Int = 0

  override def send[T, R >: P with Effect[F]](request: Request[T, R]): F[Response[T]] = {
    callCount += 1
    responseMonad.error(new UnknownHostException("Connection failed"))
  }
  override def close(): F[Unit] = delegate.close()
  override def responseMonad: MonadError[F] = delegate.responseMonad
}

@SuppressWarnings(Array("org.wartremover.warts.NonUnitStatements"))
class RetryingBackendTest extends AnyFlatSpec with Matchers {
  val port: Int = 50000 + (java.lang.Math.random() * 1000).toInt

  "RetryingBackend" should "retry failed requests multiple times" in {
    val mockBackend = new FailingRetryingBackend[IO, Any](AsyncHttpClientCatsBackend[IO]().unsafeRunSync())
    val retryingBackend = new RetryingBackend[IO, Any](mockBackend, maxRetries = 2, initialRetryDelay = 1.millis)

    val request = basicRequest
      .body("test")
      .post(uri"http://unknownHost:$port/string")

    an[Exception] should be thrownBy {
      request.send(retryingBackend).unsafeRunSync()
      ()
    }

    mockBackend.callCount should be >= 2
    mockBackend.callCount should be <= 5
  }
}