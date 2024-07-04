package com.cognite.sdk.scala.sttp

import cats.Applicative
import cats.effect.Temporal
import cats.effect.std.Queue
import cats.syntax.all._
import com.cognite.sdk.scala.common.{CdpApiException, SdkException}
import sttp.capabilities.Effect
import sttp.client3.{Request, Response, SttpBackend}
import sttp.monad.MonadError

import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.util.Random

/** When 429 Too many requests or 503 Service Unavailable error is encountered, new requests are
  * blocked for the specified duration
  *
  * @param queueOf1
  *   must be a queue with one element already placed in the queue
  */
// TODO: is this really needed compared to RetryingBackend?
// here it can be applied across multiple services and request streams
// which could be good or could be not so good.
class BackpressureThrottleBackend[F[_]: Temporal, +S](
    delegate: SttpBackend[F, S],
    queueOf1: Queue[F, Unit],
    delay: FiniteDuration
) extends SttpBackend[F, S] {

  private val permit = queueOf1

  private def processResponse(code: Int) =
    if (code === 429 || code === 503) {
      // try to take the permit and release it after the specified delay
      permit.tryTake.flatMap {
        case None => Applicative[F].unit
        case Some(_) =>
          val constantDelay = delay / 2
          val randomDelayScale = delay / 2
          val randomDelay = Random.nextInt(randomDelayScale.toMillis.toInt).millis
          Temporal[F].sleep(constantDelay + randomDelay) *>
            permit.tryOffer(()).void
      }
    } else {
      Applicative[F].unit
    }

  override def send[T, R >: S with Effect[F]](
      request: Request[T, R]
  ): F[Response[T]] =
    for {
      _ <- permit.take // Take the permit, blocking until available.
      _ <- permit.offer(()) // Put it back again so other requests may proceed.
      response <- delegate
        .send(request)
        .onError {
          case cdpError: CdpApiException => processResponse(cdpError.code)
          case SdkException(_, _, _, Some(code)) => processResponse(code)
        }
        .flatTap { response =>
          processResponse(response.code.code)
        }
    } yield response

  override def close(): F[Unit] = delegate.close()
  override def responseMonad: MonadError[F] = delegate.responseMonad
}
