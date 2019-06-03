package com.cognite.sdk.scala.v1_0

import com.cognite.sdk.scala.common.{Auth, Login}
import com.softwaremill.sttp._
import io.circe.generic.auto._

final class Client[F[_]](implicit auth: Auth, sttpBackend: SttpBackend[F, _]) {
  // TODO: auth once here instead of passing Auth down
  //val sttp1 = sttp.auth(auth)

  val login = new Login()
  val assets = new Assets()
  val events = new Events()
  val timeSeries = new TimeSeriesResource()
  val files = new Files()
}
