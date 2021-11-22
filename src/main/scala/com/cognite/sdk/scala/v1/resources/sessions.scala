// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1.resources

import com.cognite.sdk.scala.common._
import com.cognite.sdk.scala.v1._
import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker
import sttp.client3._
import sttp.client3.jsoniter_scala._

class Sessions[F[_]](val requestSession: RequestSession[F])
    extends WithRequestSession[F]
    with BaseUrl {
  import Sessions._
  override val baseUrl = uri"${requestSession.baseUrl}/sessions"

  def createWithTokenExchangeFlow(items: Items[SessionCreateWithToken]): F[Seq[Session]] =
    requestSession.post[Seq[Session], Items[Session], Items[SessionCreateWithToken]](
      items,
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
  implicit val sessionCodec: JsonValueCodec[Session] = JsonCodecMaker.make[Session]
  implicit val sessionsItemsCodec: JsonValueCodec[Items[Session]] =
    JsonCodecMaker.make[Items[Session]]
  implicit val sessionsItemsWithCursorCodec: JsonValueCodec[ItemsWithCursor[Session]] =
    JsonCodecMaker.make[ItemsWithCursor[Session]]

  implicit val sessionListCodec: JsonValueCodec[SessionList] = JsonCodecMaker.make[SessionList]
  implicit val sessionsListItemsCodec: JsonValueCodec[Items[SessionList]] =
    JsonCodecMaker.make[Items[SessionList]]

  implicit val createSessionWithTokenCodec: JsonValueCodec[SessionCreateWithToken] =
    JsonCodecMaker.make[SessionCreateWithToken]
  implicit val createSessionsWithTokenItemsCodec: JsonValueCodec[Items[SessionCreateWithToken]] =
    JsonCodecMaker.make[Items[SessionCreateWithToken]]

  implicit val createSessionWithCredentialCodec: JsonValueCodec[SessionCreateWithCredential] =
    JsonCodecMaker.make[SessionCreateWithCredential]
  implicit val createSessionsWithCredentialItemsCodec
      : JsonValueCodec[Items[SessionCreateWithCredential]] =
    JsonCodecMaker.make[Items[SessionCreateWithCredential]]

  implicit val bindSessionRequestCodec: JsonValueCodec[BindSessionRequest] =
    JsonCodecMaker.make[BindSessionRequest]

  implicit val refreshSessionRequestCodec: JsonValueCodec[RefreshSessionRequest] =
    JsonCodecMaker.make[RefreshSessionRequest]

  implicit val sessionTokenCodec: JsonValueCodec[SessionTokenResponse] =
    JsonCodecMaker.make[SessionTokenResponse]
}
