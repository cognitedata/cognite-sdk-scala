// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.common

import java.net.ConnectException

import cats.effect.Timer
import com.softwaremill.sttp.{Id, MonadError, Request, Response, SttpBackend}

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

class RetryingBackend[R[_], S](
    delegate: SttpBackend[R, S],
    maxRetries: Option[Int] = None,
    initialRetryDelay: FiniteDuration = Constants.DefaultInitialRetryDelay,
    maxRetryDelay: FiniteDuration = Constants.DefaultMaxBackoffDelay
)(implicit sleepImpl: Sleep[R])
    extends SttpBackend[R, S] {
  override def send[T](
      request: Request[T, S]
  ): R[Response[T]] =
    sendWithRetryCounter(
      request,
      maxRetries.getOrElse(Constants.DefaultMaxRetries)
    )

  @SuppressWarnings(Array("org.wartremover.warts.Recursion"))
  def sendWithRetryCounter[T](
      request: Request[T, S],
      retriesRemaining: Int,
      initialDelay: FiniteDuration = initialRetryDelay
  ): R[Response[T]] = {
    val exponentialDelay = (maxRetryDelay / 2).min(initialDelay * 2)
    val randomDelayScale = (maxRetryDelay / 2).min(initialDelay * 2).toMillis
    val nextDelay = Random.nextInt(randomDelayScale.toInt).millis + exponentialDelay

    val maybeRetry: (Option[Int], Throwable) => R[Response[T]] =
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
      case e @ (_: TimeoutException | _: ConnectException) => maybeRetry(None, e)
      //case error => responseMonad.error(error)
    }
    responseMonad.flatMap(r) { resp =>
      // This can happen when we get empty responses, as we sometimes do for
      // Service Unavailable or Bad Gateway.
      if (retriesRemaining > 0 && shouldRetry(resp.code.toInt)) {
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

  override def close(): Unit = delegate.close()
  override def responseMonad: MonadError[R] = delegate.responseMonad
}
