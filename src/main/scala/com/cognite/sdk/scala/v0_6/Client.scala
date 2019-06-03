package com.cognite.sdk.scala.v0_6

import com.cognite.sdk.scala.common.{Auth, Login}
import com.softwaremill.sttp._
import io.circe.generic.auto._

final case class Data[A](data: A)

final class Client[F[_]](implicit auth: Auth, sttpBackend: SttpBackend[F, _]) {
  // TODO: auth once here instead of passing Auth down
  //val sttp1 = sttp.auth(auth)

  val login = new Login()
  val assets = new Assets()
  val events = new Events()
  val timeSeries = new TimeSeriesResource()
  val files = new FilesResource()
}
