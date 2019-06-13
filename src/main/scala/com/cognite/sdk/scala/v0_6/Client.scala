package com.cognite.sdk.scala.v0_6

import com.cognite.sdk.scala.common.{Auth, InvalidAuthentication, Login}
import com.softwaremill.sttp._

import scala.concurrent.duration._

final case class Data[A](data: A)

final class Client[F[_]](implicit auth: Auth, sttpBackend: SttpBackend[F, _]) {
  val project: String = auth.project.getOrElse {
    implicit val sttpBackend: SttpBackend[Id, Nothing] = HttpURLConnectionBackend(
      options = SttpBackendOptions.connectionTimeout(90.seconds)
    )
    val loginStatus = new Login().status().unsafeBody
    Option(loginStatus.project).getOrElse(throw InvalidAuthentication())
  }
  val login = new Login()
  val assets = new Assets(project)
  val events = new Events(project)
  val files = new Files(project)
  val timeSeries = new TimeSeriesResource(project)
  val dataPoints = new DataPointsResourceV0_6(project)
}
