package com.cognite.sdk.scala.v06

import com.cognite.sdk.scala.common.{Auth, InvalidAuthentication, Login}
import com.softwaremill.sttp._

import scala.concurrent.duration._

final case class Data[A](data: A)

class GenericClient[F[_], S]()(implicit auth: Auth, sttpBackend: SttpBackend[F, S]) {
  val project: String = auth.project.getOrElse {
    implicit val sttpBackend: SttpBackend[Id, Nothing] = HttpURLConnectionBackend(
      options = SttpBackendOptions.connectionTimeout(90.seconds)
    )

    val loginStatus = new Login().status().unsafeBody

    if (loginStatus.project.trim.isEmpty) {
      throw InvalidAuthentication()
    } else {
      loginStatus.project
    }
  }
  val login = new Login()
  val assets = new Assets(project)
  val events = new Events(project)
  val files = new Files(project)
  val timeSeries = new TimeSeriesResource(project)
  val dataPoints = new DataPointsResourceV0_6(project)
}

final case class Client()(implicit sttpBackend: SttpBackend[Id, Nothing]) extends GenericClient[Id, Nothing]()
