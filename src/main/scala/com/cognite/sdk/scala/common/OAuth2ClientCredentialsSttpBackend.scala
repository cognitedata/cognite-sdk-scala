package com.cognite.sdk.scala.common

import com.softwaremill.sttp._
import com.softwaremill.sttp.circe._
import io.circe.Decoder
import io.circe.derivation.deriveDecoder

case class ClientCredentialsResponse(
  access_token: String,
  expires_in: String
)

object ClientCredentialsResponse {
  implicit val decoder: Decoder[ClientCredentialsResponse] = deriveDecoder
}

class OAuth2ClientCredentialsSttpBackend[R[_], -S](
    delegate: SttpBackend[R, S],
    auth: ClientCredentialsAuth,
) extends SttpBackend[R, S] {

  override def send[T](request: Request[T, S]): R[Response[T]] = {
    for {
      response <- fetchToken()
    }
  }

  def fetchToken(): R[ClientCredentialsResponse] = {
    val oauthUrl = uri"${auth.authority}/oauth/v2.0/token"

    val body = Map[String, String](
      "grant_type" -> "client_credentials",
      "scope" -> auth.scopes.mkString(" "),
      "client_id" -> auth.clientId,
      "client_secret" -> auth.clientSecret
    )

    sttp
      .header("Accept", "application/json")
      .post(oauthUrl)
      .body(body)
      .response(asJson[ClientCredentialsResponse])
  }

  override def close(): Unit = delegate.close()

  override def responseMonad: MonadError[R] = delegate.responseMonad
}