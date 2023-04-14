// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.common

import com.cognite.sdk.scala.v1.RequestSession
import sttp.client3._
import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder

final case class LoginStatus(
    user: String,
    loggedIn: Boolean,
    project: String,
    projectId: Long
)
final case class DataLoginStatus(data: LoginStatus)

class Login[F[_]](val requestSession: RequestSession[F]) {
  @SuppressWarnings(Array("org.wartremover.warts.EitherProjectionPartial"))
  implicit val loginStatusDecoder: Decoder[LoginStatus] = deriveDecoder[LoginStatus]
  implicit val dataLoginStatusDecoder: Decoder[DataLoginStatus] = deriveDecoder[DataLoginStatus]
  implicit val errorOrDataLoginStatusDecoder: Decoder[Either[CdpApiError, DataLoginStatus]] =
    EitherDecoder.eitherDecoder[CdpApiError, DataLoginStatus]
  def status(): F[LoginStatus] =
    requestSession
      .get[LoginStatus, DataLoginStatus](
        uri"${requestSession.baseUrl}/login/status",
        value => value.data
      )
}
