package com.cognite.sdk.scala.v1

import com.cognite.sdk.scala.common.{Auth, AuthProvider}
import sttp.capabilities.Effect
import sttp.client3.{Request, Response, SttpBackend}
import sttp.monad.MonadError

class AuthSttpBackend[F[_], +P](delegate: SttpBackend[F, P], authProvider: AuthProvider[F])
    extends SttpBackend[F, P] {
  override def send[T, R >: P with Effect[F]](request: Request[T, R]): F[Response[T]] =
    responseMonad.flatMap(authProvider.getAuth) { (auth: Auth) =>
      delegate.send(auth.auth(request))
    }

  override def close(): F[Unit] = delegate.close()

  override def responseMonad: MonadError[F] = delegate.responseMonad
}
