// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1.resources

import com.cognite.sdk.scala.common._
import com.cognite.sdk.scala.v1._
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import sttp.client3._
import sttp.client3.circe._

class Sessions[F[_]](val requestSession: RequestSession[F])
    extends WithRequestSession[F]
    with BaseUrl {
  import Sessions._
  override val baseUrl = uri"${requestSession.baseUrl}/sessions"

  def createWithTokenExchangeFlow(): F[Seq[Session]] =
    requestSession.post[Seq[Session], Items[Session], Items[SessionCreateWithToken]](
      Items(Seq(SessionCreateWithToken())),
      baseUrl,
      value => value.items
    )

  def createWithClientCredentialFlow(items: Items[SessionCreateWithCredential]): F[Seq[Session]] =
    requestSession.post[Seq[Session], Items[Session], Items[SessionCreateWithCredential]](
      items,
      baseUrl,
      value => value.items
    )

  def list(): F[Seq[SessionList]] =
    requestSession.get[Seq[SessionList], Items[SessionList]](
      baseUrl,
      value => value.items
    )

  def bind(bindSession: BindSessionRequest): F[SessionTokenResponse] =
    requestSession
      .post[SessionTokenResponse, SessionTokenResponse, BindSessionRequest](
        bindSession,
        uri"$baseUrl/token",
        value => value
      )

  def refresh(refreshSession: RefreshSessionRequest): F[SessionTokenResponse] =
    requestSession
      .post[SessionTokenResponse, SessionTokenResponse, RefreshSessionRequest](
        refreshSession,
        uri"$baseUrl/token",
        value => value
      )
}

object Sessions {
  implicit val sessionDecoder: Decoder[Session] = deriveDecoder[Session]
  implicit val sessionsItemsDecoder: Decoder[Items[Session]] =
    deriveDecoder[Items[Session]]
  implicit val sessionsItemsWithCursorDecoder: Decoder[ItemsWithCursor[Session]] =
    deriveDecoder[ItemsWithCursor[Session]]

  implicit val sessionListDecoder: Decoder[SessionList] = deriveDecoder[SessionList]
  implicit val sessionsListItemsDecoder: Decoder[Items[SessionList]] =
    deriveDecoder[Items[SessionList]]

  implicit val createSessionWithTokenEncoder: Encoder[SessionCreateWithToken] =
    deriveEncoder[SessionCreateWithToken]
  implicit val createSessionsWithTokenItemsEncoder: Encoder[Items[SessionCreateWithToken]] =
    deriveEncoder[Items[SessionCreateWithToken]]

  implicit val createSessionWithCredentialEncoder: Encoder[SessionCreateWithCredential] =
    deriveEncoder[SessionCreateWithCredential]
  implicit val createSessionsWithCredentialItemsEncoder
      : Encoder[Items[SessionCreateWithCredential]] =
    deriveEncoder[Items[SessionCreateWithCredential]]

  implicit val bindSessionRequestEncoder: Encoder[BindSessionRequest] =
    deriveEncoder[BindSessionRequest]

  implicit val refreshSessionRequestEncoder: Encoder[RefreshSessionRequest] =
    deriveEncoder[RefreshSessionRequest]

  implicit val sessionTokenDecoder: Decoder[SessionTokenResponse] =
    deriveDecoder[SessionTokenResponse]
}
