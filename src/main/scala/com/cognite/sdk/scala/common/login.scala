package com.cognite.sdk.scala.common

import com.cognite.sdk.scala.v1.RequestSession
import com.softwaremill.sttp._
import io.circe.Decoder
import io.circe.derivation.deriveDecoder

final case class LoginStatus(user: String, loggedIn: Boolean, project: String, projectId: Long)
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
        uri"${requestSession.baseUri}/login/status",
        value => value.data
      )
}
