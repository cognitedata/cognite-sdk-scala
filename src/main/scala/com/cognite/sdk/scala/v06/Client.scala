package com.cognite.sdk.scala.v06

import com.cognite.sdk.scala.common.{Auth, InvalidAuthentication, Login}
import com.cognite.sdk.scala.v06.resources.{
  Assets,
  DataPointsResourceV0_6,
  Events,
  Files,
  TimeSeriesResource
}
import com.softwaremill.sttp._

import scala.concurrent.duration._

final case class Data[A](data: A)

class GenericClient[F[_], _]()(implicit auth: Auth, sttpBackend: SttpBackend[F, _]) {
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
  val files = new Files[F](project)
  val timeSeries = new TimeSeriesResource(project)
  val dataPoints = new DataPointsResourceV0_6(project)
}

final case class Client()(implicit sttpBackend: SttpBackend[Id, Nothing])
    extends GenericClient[Id, Nothing]()
