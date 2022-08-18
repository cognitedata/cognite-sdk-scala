package com.cognite.sdk.scala.common

import cats.Monad
import cats.syntax.all._
import cats.effect.{Async, Clock}
import com.cognite.sdk.scala.common.internal.{CachedResource, ConcurrentCachedObject}
import com.cognite.sdk.scala.v1.GenericClient.parseResponse
import com.cognite.sdk.scala.v1.{RefreshSessionRequest, SessionTokenResponse}
import com.cognite.sdk.scala.v1.resources.Sessions.{
  refreshSessionRequestEncoder,
  sessionTokenDecoder
}
import sttp.client3._
import sttp.client3.circe.asJson
import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder
import sttp.model.Uri

//VH TODO remove this
@SuppressWarnings(Array("org.wartremover.warts.StringPlusAny"))
object OAuth2 {

  final case class ClientCredentials(
      tokenUri: Uri,
      clientId: String,
      clientSecret: String,
      scopes: List[String] = List.empty,
      cdfProjectName: String,
      audience: Option[String] = None
  ) {
    def getAuth[F[_]](refreshSecondsBeforeTTL: Long = 30)(
        implicit F: Async[F],
        clock: Clock[F],
        sttpBackend: SttpBackend[F, Any]
    ): F[TokenState] = {
      val body = Map[String, String](
        "grant_type" -> "client_credentials",
        "client_id" -> clientId,
        "client_secret" -> clientSecret,
        // Aize auth flow requires this to be empty in general
        "scope" -> scopes.mkString(" "), // Send empty scopes when it is not provided
        // AAD auth flow requires this to be empty
        "audience" -> audience.getOrElse(
          ""
        ) // Send empty audience when it is not provided
      )
      for {

        response <- basicRequest
          .header("Accept", "application/json")
          .post(tokenUri)
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
                    uri = Some(tokenUri),
                    responseCode = Some(statusCode.code)
                  )
                )
              case DeserializationException(_, error) =>
                F.raiseError(
                  new SdkException(
                    s"Failed to parse response from IdP: ${error.getMessage}"
                  )
                )
            }
        }
        acquiredAt <- clock.monotonic
        expiresAt = acquiredAt.toSeconds + payload.expires_in - refreshSecondsBeforeTTL
        _ <- F.delay(println(s" Asking for new token and it expires at ${expiresAt}"))
      } yield TokenState(payload.access_token, expiresAt, cdfProjectName)
    }
  }

  final case class Session(
      baseUrl: String,
      sessionId: Long,
      sessionKey: String,
      cdfProjectName: String,
      tokenFromVault: String
  ) {
    def getAuth[F[_]](refreshSecondsBeforeTTL: Long = 30)(
        implicit F: Async[F],
        clock: Clock[F],
        sttpBackend: SttpBackend[F, Any]
    ): F[TokenState] = {
      import sttp.client3.circe._
      val uri = uri"${baseUrl}/api/v1/projects/${cdfProjectName}/sessions/token"
      for {
        payload <- basicRequest
          .header("Accept", "application/json")
          .header("Authorization", s"Bearer ${tokenFromVault}")
          .post(uri)
          .body(RefreshSessionRequest(sessionId, sessionKey))
          .response(
            parseResponse[SessionTokenResponse, SessionTokenResponse](uri, value => value)
          )
          .send(sttpBackend)
          .map(_.body)
        acquiredAt <- clock.monotonic
        expiresAt = acquiredAt.toSeconds + payload.expiresIn - refreshSecondsBeforeTTL
      } yield TokenState(payload.accessToken, expiresAt, cdfProjectName)
    }
  }

  private def commonGetAuth[F[_]](cache: CachedResource[F, TokenState])(
      implicit F: Monad[F],
      clock: Clock[F]
  ): F[Auth] = for {
    now <- clock.monotonic
    _ <- cache.invalidateIfNeeded(_.expiresAt <= now.toSeconds)
    auth <- cache.run(state => F.pure(OidcTokenAuth(state.token, state.cdfProjectName)))
  } yield auth

  class ClientCredentialsProvider[F[_]] private (
      cache: CachedResource[F, TokenState]
  )(
      implicit F: Monad[F],
      clock: Clock[F]
  ) extends AuthProvider[F]
      with Serializable {
    def getAuth: F[Auth] = commonGetAuth(cache)
  }

  class SessionProvider[F[_]] private (
      cache: CachedResource[F, TokenState]
  )(implicit F: Monad[F], clock: Clock[F])
      extends AuthProvider[F]
      with Serializable {
    def getAuth: F[Auth] = commonGetAuth(cache)
  }

  // scalastyle:off method.length
  object ClientCredentialsProvider {
    def apply[F[_]](
        credentials: ClientCredentials,
        refreshSecondsBeforeTTL: Long = 30,
        maybeCacheToken: Option[TokenState] = None
    )(
        implicit F: Async[F],
        clock: Clock[F],
        sttpBackend: SttpBackend[F, Any]
    ): F[ClientCredentialsProvider[F]] = {
      val authenticate: F[TokenState] =
        for {
          now <- clock.monotonic
          res <- maybeCacheToken match {
            case Some(originalToken)
                if now.toSeconds < (originalToken.expiresAt - refreshSecondsBeforeTTL) =>
              // VH TODO Remove println
              F.delay(println(s" Use static now at ${now.toSeconds}")) *>
                F.delay(originalToken)
            case _ =>
              credentials.getAuth()
          }
        } yield res
      ConcurrentCachedObject(authenticate).map(new ClientCredentialsProvider[F](_))
    }
  }
  // scalastyle:on method.length

  object SessionProvider {
    def apply[F[_]](
        session: Session,
        refreshSecondsBeforeTTL: Long = 30,
        maybeCacheToken: Option[TokenState] = None
    )(
        implicit F: Async[F],
        clock: Clock[F],
        sttpBackend: SttpBackend[F, Any]
    ): F[SessionProvider[F]] = {
      val authenticate: F[TokenState] =
        for {
          now <- clock.monotonic
          res <- maybeCacheToken match {
            case Some(originalToken)
                if now.toSeconds < (originalToken.expiresAt - refreshSecondsBeforeTTL) =>
              F.delay(originalToken)
            case _ =>
              session.getAuth()
          }
        } yield res
      ConcurrentCachedObject(authenticate).map(x => new SessionProvider[F](x))
    }
  }

  private final case class ClientCredentialsResponse(access_token: String, expires_in: Long)
  private object ClientCredentialsResponse {
    implicit val decoder: Decoder[ClientCredentialsResponse] = deriveDecoder
  }

  final case class TokenState(token: String, expiresAt: Long, cdfProjectName: String)
}
