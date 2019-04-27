package com.cognite.sdk.scala.common

import com.cognite.sdk.scala.v0_6.Data
import com.softwaremill.sttp._
import com.softwaremill.sttp.circe._
import io.circe.generic.auto._

final case class LoginStatus(user: String, loggedIn: Boolean, project: String, projectId: Long)

class Login[F[_]](implicit auth: Auth, sttpBackend: SttpBackend[F, _]) {
  @SuppressWarnings(Array("org.wartremover.warts.EitherProjectionPartial"))
  def status(): F[Response[LoginStatus]] =
    sttp
      .auth(auth)
      .get(uri"https://api.cognitedata.com/login/status")
      .response(asJson[Data[LoginStatus]])
      .mapResponse(_.right.get.data)
      .send()
}
