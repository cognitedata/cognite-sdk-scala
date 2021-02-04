package com.cognite.sdk.scala.common

import java.util.concurrent.TimeUnit

import cats.Monad
import cats.syntax.all._
import cats.effect.{Clock, ConcurrentEffect}
import com.cognite.sdk.scala.common.internal.{CachedResource, ConcurrentCachedObject}
import com.softwaremill.sttp._
import com.softwaremill.sttp.circe.asJson
import io.circe.Decoder
import io.circe.derivation.deriveDecoder

object OAuth2 {
  final case class ClientCredentials(
      tokenUri: Uri,
      clientId: String,
      clientSecret: String,
      scopes: List[String]
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
        auth <- cache.run(state => F.pure(BearerTokenAuth(state.token)))
      } yield auth
  }

  object ClientCredentialsProvider {
    def apply[F[_]](
        credentials: ClientCredentials,
        refreshSecondsBeforeTTL: Long = 30
    )(
        implicit F: ConcurrentEffect[F],
        clock: Clock[F],
        sttpBackend: SttpBackend[F, Nothing]
    ): F[ClientCredentialsProvider[F]] = {
      val authenticate: F[TokenState] = {
        val body = Map[String, String](
          "grant_type" -> "client_credentials",
          "scope" -> credentials.scopes.mkString(" "),
          "client_id" -> credentials.clientId,
          "client_secret" -> credentials.clientSecret
        )
        for {
          response <- sttp
            .header("Accept", "application/json")
            .post(credentials.tokenUri)
            .body(body)
            .response(asJson[ClientCredentialsResponse])
            .send()
          payload <- response.body match {
            case Right(Right(response)) => F.pure(response)
            case Left(body) => // Non-2xx response
              F.raiseError[ClientCredentialsResponse](
                new SdkException(
                  body,
                  uri = Some(credentials.tokenUri),
                  responseCode = Some(response.code)
                )
              )
            case Right(Left(deserializationError)) =>
              F.raiseError[ClientCredentialsResponse](
                new SdkException(
                  s"Failed to parse response from IdP: ${deserializationError.message}"
                )
              )
          }
          acquiredAt <- clock.monotonic(TimeUnit.SECONDS)
          expiresAt = acquiredAt + payload.expires_in - refreshSecondsBeforeTTL
        } yield TokenState(payload.access_token, expiresAt)
      }

      ConcurrentCachedObject(authenticate).map(new ClientCredentialsProvider[F](_))
    }
  }

  private final case class ClientCredentialsResponse(access_token: String, expires_in: Long)
  private object ClientCredentialsResponse {
    implicit val decoder: Decoder[ClientCredentialsResponse] = deriveDecoder
  }

  private final case class TokenState(token: String, expiresAt: Long)
}
