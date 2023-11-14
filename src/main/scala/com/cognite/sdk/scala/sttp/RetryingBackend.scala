// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.sttp

import cats.effect.Temporal
import com.cognite.sdk.scala.common.{CdpApiException, Constants, SdkException}
import com.cognite.sdk.scala.v1.GenericClient
import sttp.capabilities.Effect
import sttp.client3.{Request, Response, SttpBackend, SttpClientException}
import sttp.model.StatusCode
import sttp.monad.MonadError

import java.net.ConnectException
import scala.concurrent.TimeoutException
import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.util.Random

trait ShouldRetryPredicate {
  def shouldRetry[T, R](request: Request[T, R], responseCode: StatusCode): Boolean
}

class RetryingBackend[F[_], +P](
    delegate: SttpBackend[F, P],
    maxRetries: Int = Constants.DefaultMaxRetries,
    initialRetryDelay: FiniteDuration = Constants.DefaultInitialRetryDelay,
    maxRetryDelay: FiniteDuration = Constants.DefaultMaxBackoffDelay,
    shouldRetry: ShouldRetryPredicate = RetryingBackend.DefaultShouldRetryPredicate
)(implicit temporal: Temporal[F])
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

    val maybeRetry: (Option[StatusCode], Throwable) => F[Response[T]] =
      (code: Option[StatusCode], exception: Throwable) =>
        if (retriesRemaining > 0 && code.forall(shouldRetry.shouldRetry(request, _))) {
          responseMonad.flatMap(temporal.sleep(initialDelay))(_ =>
            sendWithRetryCounter(request, retriesRemaining - 1, nextDelay)
          )
        } else {
          responseMonad.error(exception)
        }

    val r = responseMonad.handleError(delegate.send(request)) {
      case cdpError: CdpApiException => maybeRetry(Some(StatusCode(cdpError.code)), cdpError)
      case sdkException @ SdkException(_, _, _, code @ Some(_)) =>
        maybeRetry(code.map(StatusCode(_)), sdkException)
      case e @ (_: TimeoutException | _: ConnectException | _: SttpClientException) =>
        maybeRetry(None, e)
    }
    responseMonad.flatMap(r) { resp =>
      // This can happen when we get empty responses, as we sometimes do for
      // Service Unavailable or Bad Gateway.
      if (retriesRemaining > 0 && shouldRetry.shouldRetry(request, resp.code)) {
        responseMonad.flatMap(temporal.sleep(initialDelay))(_ =>
          sendWithRetryCounter(request, retriesRemaining - 1, nextDelay)
        )
      } else {
        responseMonad.unit(resp)
      }
    }
  }

  override def close(): F[Unit] = delegate.close()
  override def responseMonad: MonadError[F] = delegate.responseMonad
}

object RetryingBackend {
  val DefaultShouldRetryPredicate: ShouldRetryPredicate = new ShouldRetryPredicate {
    override def shouldRetry[T, R](request: Request[T, R], statusCode: StatusCode): Boolean =
      statusCode.code match {
        case 429 | 500 | 502 | 503 | 504 => true
        case 409
            // 409 in dms can be transient and retriable
            if request.tag(GenericClient.RESOURCE_TYPE_TAG).contains(GenericClient.DATAMODELS) =>
          true
        case _ => false
      }
  }
}
