// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1

final case class Session(
    id: Long,
    `type`: String,
    status: String,
    nonce: String,
    clientId: Option[String]
)

final case class SessionCreateWithToken(
    tokenExchange: Boolean = true
)

final case class SessionCreateWithCredential(
    clientId: String,
    clientSecret: String
)

final case class SessionList(
    id: Long,
    `type`: String,
    status: String,
    creationTime: Long,
    expirationTime: Long,
    clientId: String
)

final case class BindSessionRequest(
    nonce: String
)

final case class RefreshSessionRequest(
    sessionId: Long,
    sessionKey: String
)

final case class SessionTokenResponse(
    id: Long,
    accessToken: String,
    expiresIn: Long,
    mustRefreshIn: Option[Long],
    sessionKey: Option[String]
)
