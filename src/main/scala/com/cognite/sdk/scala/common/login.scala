// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.common

import com.cognite.sdk.scala.v1.RequestSession
import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker
import sttp.client3._

final case class LoginStatus(
    user: String,
    loggedIn: Boolean,
    project: String,
    projectId: Long,
    apiKeyId: Option[Long] = None
)
final case class DataLoginStatus(data: LoginStatus)

class Login[F[_]](val requestSession: RequestSession[F]) {
  @SuppressWarnings(Array("org.wartremover.warts.EitherProjectionPartial"))
  implicit val loginStatusCodec: JsonValueCodec[LoginStatus] = JsonCodecMaker.make[LoginStatus]
  implicit val dataLoginStatusCodec: JsonValueCodec[DataLoginStatus] = JsonCodecMaker.make[DataLoginStatus]
  implicit val errorOrDataLoginStatusCodec: JsonValueCodec[Either[CdpApiError, DataLoginStatus]] =
    JsonCodecMaker.make[Either[CdpApiError, DataLoginStatus]]
  def status(): F[LoginStatus] =
    requestSession
      .get[LoginStatus, DataLoginStatus](
        uri"${requestSession.baseUrl}/login/status",
        value => value.data
      )
}
