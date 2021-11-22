// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.common

import sttp.client3.RequestT
import sttp.monad.MonadError

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

  implicit class AuthSttpExtension[U[_], T, -R](val r: RequestT[U, T, R]) {
    def auth(auth: Auth): RequestT[U, T, R] =
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

final case class OidcTokenAuth(bearerToken: String, projectName: String) extends Auth {
  override val project: Option[String] = Some(projectName)

  def auth[U[_], T, S](r: RequestT[U, T, S]): RequestT[U, T, S] =
    r.header("Authorization", s"Bearer $bearerToken")
      .header("project", projectName)
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
  def apply[F[_]: MonadError](auth: Auth): AuthProvider[F] = new AuthProvider[F] {
    def getAuth: F[Auth] = MonadError[F].unit(auth)
  }
}
