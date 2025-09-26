package com.cognite.sdk.scala.sttp

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import sttp.client3._
import sttp.monad.MonadError
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import sttp.capabilities.Effect
import sttp.client3.impl.cats.implicits.monadError

import java.net.UnknownHostException
import scala.concurrent.duration.DurationInt

@SuppressWarnings(Array("org.wartremover.warts.Var", "org.wartremover.warts.AsInstanceOf"))
class FailingRetryingBackend[F[_], +P](val shouldSucceed: Boolean = false)(implicit val responseMonad: MonadError[F]) extends SttpBackend[F, P] {
  var callCount: Int = 0

  override def send[T, R >: P with Effect[F]](request: Request[T, R]): F[Response[T]] = {
    callCount += 1
    if(shouldSucceed)
      responseMonad.unit(Response.ok(Right("mock success").asInstanceOf[T]))
    else
      responseMonad.error(new UnknownHostException("Connection failed"))
  }
  override def close(): F[Unit] = responseMonad.unit(())
}

@SuppressWarnings(Array("org.wartremover.warts.NonUnitStatements"))
class RetryingBackendTest extends AnyFlatSpec with Matchers {
  val port: Int = 50000 + (java.lang.Math.random() * 1000).toInt

  "RetryingBackend" should "retry failed requests multiple times" in {
    val mockBackend = new FailingRetryingBackend[IO, Any]
    val retryingBackend = new RetryingBackend[IO, Any](mockBackend, maxRetries = 2, initialRetryDelay = 1.millis)

    //request doesn't matter, underlying backend just returns a failure
    val request = basicRequest
      .body("test")
      .post(uri"http://host:$port/string")

    an[UnknownHostException] should be thrownBy {
      request.send(retryingBackend).unsafeRunSync()
      ()
    }

    mockBackend.callCount should be(3)
  }

  it should "not retry successful requests multiple times" in {
    val mockBackend = new FailingRetryingBackend[IO, Any](shouldSucceed = true)
    val retryingBackend = new RetryingBackend[IO, Any](mockBackend, maxRetries = 2, initialRetryDelay = 1.millis)

    //request doesn't matter, underlying backend just returns a success
    val request = basicRequest
      .body("test")
      .post(uri"http://host:$port/string")

    request.send(retryingBackend).unsafeRunSync()


    mockBackend.callCount shouldBe 1
  }
}