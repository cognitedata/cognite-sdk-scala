package com.cognite.sdk.scala.common

import java.util.concurrent.TimeUnit
import cats.Monad
import cats.syntax.all._
import cats.effect.{Clock, ConcurrentEffect}
import com.cognite.sdk.scala.common.internal.{CachedResource, ConcurrentCachedObject}
import sttp.client3._
import sttp.client3.circe.asJson
import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder
import sttp.model.Uri

object OAuth2 {
  final case class ClientCredentials(
      tokenUri: Uri,
      clientId: String,
      clientSecret: String,
      scopes: List[String] = List.empty,
      cdfProjectName: String,
      audience: Option[String] = None
  )

  class ClientCredentialsProvider[F[_]] private (
      cache: CachedResource[F, TokenState]
  )(
      implicit F: Monad[F],
      clock: Clock[F]
  ) extends AuthProvider[F]
      with Serializable {
    def getAuth: F[Auth] =
      for {
        now <- clock.monotonic(TimeUnit.SECONDS)
        _ <- cache.invalidateIfNeeded(_.expiresAt <= now)
        auth <- cache.run(state => F.pure(OidcTokenAuth(state.token, state.cdfProjectName)))
      } yield auth
  }

  // scalastyle:off method.length
  object ClientCredentialsProvider {
    def apply[F[_]](
        credentials: ClientCredentials,
        refreshSecondsBeforeTTL: Long = 30
    )(
        implicit F: ConcurrentEffect[F],
        clock: Clock[F],
        sttpBackend: SttpBackend[F, Any]
    ): F[ClientCredentialsProvider[F]] = {
      val authenticate: F[TokenState] = {
        val body = Map[String, String](
          "grant_type" -> "client_credentials",
          "client_id" -> credentials.clientId,
          "client_secret" -> credentials.clientSecret,
          // Aize auth flow requires this to be empty in general
          "scope" -> credentials.scopes.mkString(" "), // Send empty scopes when it is not provided
          // AAD auth flow requires this to be empty
          "audience" -> credentials.audience.getOrElse(
            ""
          ) // Send empty audience when it is not provided
        )
        for {
          response <- basicRequest
            .header("Accept", "application/json")
            .post(credentials.tokenUri)
            .body(body)
            .response(asJson[ClientCredentialsResponse])
            .send(sttpBackend)
          payload <- response.body match {
            case Right(response) => F.pure(response)
            case Left(responseException) => // Non-2xx response
              responseException match {
                case HttpError(body, statusCode) =>
                  F.raiseError[ClientCredentialsResponse](
                    new SdkException(
                      body,
                      uri = Some(credentials.tokenUri),
                      responseCode = Some(statusCode.code)
                    )
                  )
                case DeserializationException(_, error) =>
                  F.raiseError[ClientCredentialsResponse](
                    new SdkException(
                      s"Failed to parse response from IdP: ${error.getMessage}"
                    )
                  )
              }
          }
          acquiredAt <- clock.monotonic(TimeUnit.SECONDS)
          expiresAt = acquiredAt + payload.expires_in - refreshSecondsBeforeTTL
        } yield TokenState(payload.access_token, expiresAt, credentials.cdfProjectName)
      }

      ConcurrentCachedObject(authenticate).map(new ClientCredentialsProvider[F](_))
    }
  }
  // scalastyle:on method.length

  private final case class ClientCredentialsResponse(access_token: String, expires_in: Long)
  private object ClientCredentialsResponse {
    implicit val decoder: Decoder[ClientCredentialsResponse] = deriveDecoder
  }

  private final case class TokenState(token: String, expiresAt: Long, cdfProjectName: String)
}
