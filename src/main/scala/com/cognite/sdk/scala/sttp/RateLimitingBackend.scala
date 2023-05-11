package com.cognite.sdk.scala.sttp

import cats.effect.IO
import cats.effect.kernel.{GenConcurrent, MonadCancel}
import cats.effect.std.Semaphore
import cats.effect.unsafe.IORuntime
import sttp.capabilities.Effect
import sttp.client3.{Request, Response, SttpBackend}
import sttp.monad.MonadError

class RateLimitingBackend[F[_], +P] private (
    delegate: SttpBackend[F, P],
    semaphore: Semaphore[F]
)(implicit monadCancel: MonadCancel[F, Throwable])
    extends SttpBackend[F, P] {
  override def send[T, R >: P with Effect[F]](
      request: Request[T, R]
  ): F[Response[T]] =
    // This will allow only the specified number of request run in parallel
    // While it's not the smartest way to react to rate limiting, this will
    // actually respond to backpressure when used with the RetryingBackend:
    // The 503 rate limit response is retried, but with a delay. It will also
    // block the semaphore for new requests, so it will reduce the rate
    semaphore.permit.use { _ =>
      delegate.send(request)
    }

  override def close(): F[Unit] = delegate.close()
  override def responseMonad: MonadError[F] = delegate.responseMonad
}

object RateLimitingBackend {
  def apply[S](
      delegate: SttpBackend[IO, S],
      maxParallelRequests: Int
  )(implicit ioRuntime: IORuntime): RateLimitingBackend[IO, S] =
    new RateLimitingBackend[IO, S](
      delegate,
      Semaphore[IO](maxParallelRequests.toLong).unsafeRunSync()
    )

  def apply[F[_], S](
      delegate: SttpBackend[F, S],
      maxParallelRequests: Int
  )(
      implicit monadCancel: MonadCancel[F, Throwable],
      genConcurrent: GenConcurrent[F, _]
  ): F[RateLimitingBackend[F, S]] =
    monadCancel.map(Semaphore[F](maxParallelRequests.toLong))(
      new RateLimitingBackend[F, S](
        delegate,
        _
      )
    )
}
