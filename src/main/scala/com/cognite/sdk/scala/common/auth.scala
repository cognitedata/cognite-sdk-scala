// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.common

import cats.{Applicative, Monad}
import com.softwaremill.sttp.RequestT

final case class InvalidAuthentication() extends Throwable(s"Invalid authentication")

sealed trait Auth {
  val project: Option[String] = None
  def auth[U[_], T, S](r: RequestT[U, T, S]): RequestT[U, T, S]
}

object Auth {
  val apiKeyEnvironmentVariable: String = "COGNITE_API_KEY"
  val defaultAuth: Auth =
    Option(System.getenv(apiKeyEnvironmentVariable))
      .map(ApiKeyAuth(_, None))
      .getOrElse[Auth](NoAuthentication())

  implicit class AuthSttpExtension[U[_], T, +S](val r: RequestT[U, T, S]) {
    def auth(auth: Auth): RequestT[U, T, S] =
      auth.auth(r)
  }
}

final case class NoAuthentication() extends Auth {
  def auth[U[_], T, S](r: RequestT[U, T, S]): RequestT[U, T, S] =
    throw new SdkException(
      s"Authentication not provided and environment variable ${Auth.apiKeyEnvironmentVariable} not set"
    )
}

final case class ApiKeyAuth(apiKey: String, override val project: Option[String] = None)
    extends Auth {
  def auth[U[_], T, S](r: RequestT[U, T, S]): RequestT[U, T, S] =
    r.header("api-key", apiKey)
}

final case class BearerTokenAuth(bearerToken: String, override val project: Option[String] = None)
    extends Auth {
  def auth[U[_], T, S](r: RequestT[U, T, S]): RequestT[U, T, S] =
    r.header("Authorization", s"Bearer $bearerToken")
}

final case class OIDCTokenAuth(bearerToken: String, override val project: Option[String] = None)
    extends Auth {
  def auth[U[_], T, S](r: RequestT[U, T, S]): RequestT[U, T, S] =
    project match {
      case Some(proj) =>
        r.header("Authorization", s"Bearer $bearerToken")
          .header("project", proj)
      case _ =>
        throw new SdkException(
          s"Invalid Auth: Can't create OIDCTokenAuth since project header is not provided"
        )
    }

}

final case class TicketAuth(authTicket: String, override val project: Option[String] = None)
    extends Auth {
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
