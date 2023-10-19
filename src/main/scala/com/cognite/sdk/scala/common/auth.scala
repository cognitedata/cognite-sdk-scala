// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.common

import cats.{Applicative, Monad}
import sttp.client3.RequestT

final case class InvalidAuthentication() extends Throwable(s"Invalid authentication")

sealed trait Auth {
  def auth[U[_], T, S](r: RequestT[U, T, S]): RequestT[U, T, S]
}

object Auth {
  implicit class AuthSttpExtension[U[_], T, -R](val r: RequestT[U, T, R]) {
    def auth(auth: Auth): RequestT[U, T, R] =
      auth.auth(r)
  }
}

final case class NoAuthentication() extends Auth {
  def auth[U[_], T, S](r: RequestT[U, T, S]): RequestT[U, T, S] =
    throw new SdkException(
      s"Authentication not provided"
    )
}

final case class BearerTokenAuth(bearerToken: String) extends Auth {
  def auth[U[_], T, S](r: RequestT[U, T, S]): RequestT[U, T, S] =
    r.header("Authorization", s"Bearer $bearerToken")
}

final case class TicketAuth(authTicket: String) extends Auth {
  def auth[U[_], T, S](r: RequestT[U, T, S]): RequestT[U, T, S] =
    r.header("auth-ticket", authTicket)
}

trait AuthProvider[F[_]] {
  def getAuth: F[Auth]
}

object AuthProvider {
  def apply[F[_]: Monad](auth: Auth): AuthProvider[F] = new AuthProvider[F] {
    def getAuth: F[Auth] = Applicative[F].pure(auth)
  }
}
