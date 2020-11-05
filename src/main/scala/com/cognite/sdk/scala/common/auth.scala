// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.common

import com.softwaremill.sttp.{MonadError, Request, RequestT, Response, SttpBackend}

final case class InvalidAuthentication() extends Throwable(s"Invalid authentication")

sealed trait Auth {
  val project: Option[String] = None
  def middleware[F[_], S](backend: SttpBackend[F, S]): SttpBackend[F, S]
}

/** For simple authentication schemes that only apply a pure function to the request. */
sealed trait PureAuth extends Auth {
  def auth[U[_], T, S](r: RequestT[U, T, S]): RequestT[U, T, S]

  final override def middleware[F[_], S](delegate: SttpBackend[F, S]): SttpBackend[F, S] =
    new SttpBackend[F, S] {
      override def send[T](request: Request[T, S]): F[Response[T]] =
        delegate.send(auth(request))

      override def close(): Unit =
        delegate.close()

      override def responseMonad: MonadError[F] =
        delegate.responseMonad
    }
}

object Auth {
  val apiKeyEnvironmentVariable: String = "COGNITE_API_KEY"
  val defaultAuth: Auth =
    Option(System.getenv(apiKeyEnvironmentVariable))
      .map(ApiKeyAuth(_, None))
      .getOrElse[Auth](NoAuthentication())
}

final case class NoAuthentication() extends PureAuth {
  def auth[U[_], T, S](r: RequestT[U, T, S]): RequestT[U, T, S] =
    throw new SdkException(
      s"Authentication not provided and environment variable ${Auth.apiKeyEnvironmentVariable} not set"
    )
}

final case class ApiKeyAuth(apiKey: String, override val project: Option[String] = None)
    extends PureAuth {
  def auth[U[_], T, S](r: RequestT[U, T, S]): RequestT[U, T, S] =
    r.header("api-key", apiKey)
}

final case class BearerTokenAuth(bearerToken: String, override val project: Option[String] = None)
    extends PureAuth {
  def auth[U[_], T, S](r: RequestT[U, T, S]): RequestT[U, T, S] =
    r.header("Authorization", s"Bearer $bearerToken")
}

final case class TicketAuth(authTicket: String, override val project: Option[String] = None)
    extends PureAuth {
  def auth[U[_], T, S](r: RequestT[U, T, S]): RequestT[U, T, S] =
    r.header("auth-ticket", authTicket)
}

final case class ClientCredentialsAuth(
    authority: String = "https://login.microsoftonline.com/",
    tenant: String,
    clientId: String,
    clientSecret: String,
    scopes: List[String]
) extends Auth
{
  override def middleware[F[_], S](backend: SttpBackend[F, S]): SttpBackend[F, S] =
    new OAuth2ClientCredentialsSttpBackend[F, S](backend, this)
}
