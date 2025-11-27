package com.cognite.sdk.scala.v1

import natchez.Trace
import sttp.capabilities.Effect
import sttp.client3.{Request, Response, SttpBackend}
import sttp.model.Header
import sttp.monad.MonadError

class TraceSttpBackend[F[_]: Trace, +P](delegate: SttpBackend[F, P]) extends SttpBackend[F, P] {

  def sendImpl[T, R >: P with Effect[F]](
      request: Request[T, R]
  )(implicit monad: MonadError[F]): F[Response[T]] =
    Trace[F].span("sttp-client-request") {
      import sttp.monad.syntax._
      for {
        knl <- Trace[F].kernel
        _ <- Trace[F].put(
          "client.http.uri" -> request.uri.toString(),
          "client.http.method" -> request.method.toString
        )
        response <- delegate.send(
          request.headers(
            knl.toHeaders.map { case (k, v) => Header(k.toString, v) }.toSeq: _*
          )
        )
        _ <- Trace[F].put("client.http.status_code" -> response.code.toString())
      } yield response
    }
  override def send[T, R >: P with Effect[F]](request: Request[T, R]): F[Response[T]] =
    sendImpl(request)(responseMonad)

  override def close(): F[Unit] = delegate.close()

  override def responseMonad: MonadError[F] = delegate.responseMonad
}
