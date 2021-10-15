// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.common

import cats.Id

import java.net.ConnectException
import cats.effect.Timer
import sttp.capabilities.Effect
import sttp.client3.{Request, Response, SttpBackend, SttpClientException}
import sttp.monad.MonadError

import scala.concurrent.duration.{FiniteDuration, _}
import scala.concurrent.TimeoutException
import scala.util.Random

trait Sleep[R[_]] {
  def sleep(sleepDuration: FiniteDuration): R[Unit]
}

object Sleep {
  class CatsSleep[F[_]](implicit T: Timer[F]) extends Sleep[F] {
    override def sleep(sleepDuration: FiniteDuration): F[Unit] =
      T.sleep(sleepDuration)
  }

  implicit def catsSleep[R[_]](implicit T: Timer[R]): Sleep[R] =
    new CatsSleep[R]

  implicit val idSleep: Sleep[Id] = new Sleep[Id] {
    override def sleep(sleepDuration: FiniteDuration): Id[Unit] = {
      Thread.sleep(sleepDuration.toMillis)
      ()
    }
  }
}

class RetryingBackend[F[_], +P](
    delegate: SttpBackend[F, P],
    maxRetries: Int = Constants.DefaultMaxRetries,
    initialRetryDelay: FiniteDuration = Constants.DefaultInitialRetryDelay,
    maxRetryDelay: FiniteDuration = Constants.DefaultMaxBackoffDelay
)(implicit sleepImpl: Sleep[F])
    extends SttpBackend[F, P] {
  override def send[T, R >: P with Effect[F]](request: Request[T, R]): F[Response[T]] =
    sendWithRetryCounter(request, maxRetries)

  @SuppressWarnings(Array("org.wartremover.warts.Recursion"))
  def sendWithRetryCounter[T, R >: P with Effect[F]](
      request: Request[T, R],
      retriesRemaining: Int,
      initialDelay: FiniteDuration = initialRetryDelay
  ): F[Response[T]] = {
    val exponentialDelay = (maxRetryDelay / 2).min(initialDelay * 2)
    val randomDelayScale = (maxRetryDelay / 2).min(initialDelay * 2).toMillis
    val nextDelay = Random.nextInt(randomDelayScale.toInt).millis + exponentialDelay

    val maybeRetry: (Option[Int], Throwable) => F[Response[T]] =
      (code: Option[Int], exception: Throwable) =>
        if (retriesRemaining > 0 && code.forall(shouldRetry)) {
          responseMonad.flatMap(sleepImpl.sleep(initialDelay))(_ =>
            sendWithRetryCounter(request, retriesRemaining - 1, nextDelay)
          )
        } else {
          responseMonad.error(exception)
        }

    val r = responseMonad.handleError(delegate.send(request)) {
      case cdpError: CdpApiException => maybeRetry(Some(cdpError.code), cdpError)
      case sdkException @ SdkException(_, _, _, code @ Some(_)) => maybeRetry(code, sdkException)
      case e @ (_: TimeoutException | _: ConnectException | _: SttpClientException) =>
        maybeRetry(None, e)
    }
    responseMonad.flatMap(r) { resp =>
      // This can happen when we get empty responses, as we sometimes do for
      // Service Unavailable or Bad Gateway.
      if (retriesRemaining > 0 && shouldRetry(resp.code.code)) {
        responseMonad.flatMap(sleepImpl.sleep(initialDelay))(_ =>
          sendWithRetryCounter(request, retriesRemaining - 1, nextDelay)
        )
      } else {
        responseMonad.unit(resp)
      }
    }
  }

  private def shouldRetry(status: Int): Boolean =
    status match {
      case 429 | 401 | 500 | 502 | 503 | 504 => true
      case _ => false
    }

  override def close(): F[Unit] = delegate.close()
  override def responseMonad: MonadError[F] = delegate.responseMonad
}
