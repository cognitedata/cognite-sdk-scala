package com.cognite.sdk.scala.common

import com.softwaremill.sttp.RequestT

final case class InvalidAuthentication() extends Throwable(s"Invalid authentication")

sealed trait Auth {
  val project: Option[String] = None
  def auth[U[_], T, S](r: RequestT[U, T, S]): RequestT[U, T, S]
}

object Auth {
  val apiKeyEnvironmentVariable = "COGNITE_API_KEY"
  implicit val auth: Auth =
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
    throw new RuntimeException(
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

final case class AuthTicketAuth(authTicket: String, override val project: Option[String] = None)
    extends Auth {
  def auth[U[_], T, S](r: RequestT[U, T, S]): RequestT[U, T, S] =
    r.header("auth-ticket", authTicket)
}
