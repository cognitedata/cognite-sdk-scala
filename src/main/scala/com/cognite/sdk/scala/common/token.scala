// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.common

import com.cognite.sdk.scala.v1.RequestSession
import sttp.client3._
import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder

final case class ProjectDetails(projectUrlName: String, groups: Seq[Long])

final case class TokenInspectResponse(
    subject: String,
    projects: Seq[ProjectDetails]
)

class Token[F[_]](val requestSession: RequestSession[F]) {
  @SuppressWarnings(Array("org.wartremover.warts.EitherProjectionPartial"))
  implicit val projectDetailsDecoder: Decoder[ProjectDetails] = deriveDecoder[ProjectDetails]
  implicit val tokenInspectResponseDecoder: Decoder[TokenInspectResponse] =
    deriveDecoder[TokenInspectResponse]
  implicit val errorOrTokenInspectResponse: Decoder[Either[CdpApiError, TokenInspectResponse]] =
    EitherDecoder.eitherDecoder[CdpApiError, TokenInspectResponse]
  def inspect(): F[TokenInspectResponse] =
    requestSession
      .get[TokenInspectResponse, TokenInspectResponse](
        uri"${requestSession.baseUrl}/api/playground/token/inspect",
        value => value
      )
}
