// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.common

import com.cognite.sdk.scala.v1.RequestSession
import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker
import sttp.client3._

final case class ProjectDetails(projectUrlName: String, groups: Seq[Long])

final case class TokenInspectResponse(
    subject: String,
    projects: Seq[ProjectDetails]
)

class Token[F[_]](val requestSession: RequestSession[F]) {
  @SuppressWarnings(Array("org.wartremover.warts.EitherProjectionPartial"))
  implicit val projectDetailsCodec: JsonValueCodec[ProjectDetails] = JsonCodecMaker.make[ProjectDetails]
  implicit val tokenInspectResponseCodec: JsonValueCodec[TokenInspectResponse] =
    JsonCodecMaker.make[TokenInspectResponse]
  implicit val errorOrTokenInspectResponse: JsonValueCodec[Either[CdpApiError, TokenInspectResponse]] =
    JsonCodecMaker.make[Either[CdpApiError, TokenInspectResponse]]
  def inspect(): F[TokenInspectResponse] =
    requestSession
      .get[TokenInspectResponse, TokenInspectResponse](
        uri"${requestSession.baseUrl}/api/v1/token/inspect",
        value => value
      )
}
