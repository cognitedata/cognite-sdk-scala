package com.cognite.sdk.scala.common

import com.cognite.sdk.scala.v1.RequestSession
import com.softwaremill.sttp._
import com.softwaremill.sttp.circe._
import io.circe.derivation.deriveDecoder

final case class LoginStatus(user: String, loggedIn: Boolean, project: String, projectId: Long)
final case class DataLoginStatus(data: LoginStatus)
class Login[F[_]](val requestSession: RequestSession[F]) {
  @SuppressWarnings(Array("org.wartremover.warts.EitherProjectionPartial"))
  implicit val loginStatusDecoder = deriveDecoder[LoginStatus]
  implicit val dataLoginStatusDecoder = deriveDecoder[DataLoginStatus]
  def status(): F[LoginStatus] =
    requestSession
      .send { request =>
        request
          .get(uri"${requestSession.baseUri}/login/status")
          .response(asJson[DataLoginStatus])
          .mapResponse {
            case Left(value) => throw value.error
            case Right(value) => value.data
          }
      }
}
