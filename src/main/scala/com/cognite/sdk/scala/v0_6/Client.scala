package com.cognite.sdk.scala.v0_6

import com.cognite.sdk.scala.common.{Auth, Login}
import com.softwaremill.sttp._
import scala.concurrent.duration._

final case class Data[A](data: A)

final class Client[F[_]](implicit auth: Auth, sttpBackend: SttpBackend[F, _]) {
  // TODO: auth once here instead of passing Auth down
  //val sttp1 = sttp.auth(auth)

  private val project = {
    implicit val sttpBackend: SttpBackend[Id, Nothing] = HttpURLConnectionBackend(
      options = SttpBackendOptions.connectionTimeout(90.seconds)
    )
    new Login().status().unsafeBody.project
  }
  val login = new Login()
  val assets = new Assets(project)
  val events = new Events(project)
  val files = new Files(project)
  val timeSeries = new TimeSeriesResource(project)
  val dataPoints = new DataPointsResourceV0_6(project)
}
