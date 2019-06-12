package com.cognite.sdk.scala.v1_0

import com.cognite.sdk.scala.common.{Auth, Login}
import com.softwaremill.sttp._
import scala.concurrent.duration._

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
  val timeSeries = new TimeSeriesResourceRead(project)
  val dataPoints = new DataPointsResourceV1(project)
  val rawDatabases = new RawDatabases(project)
  def rawTables(database: String): RawTables[F] = new RawTables(project, database)
  def rawRows(database: String, table: String): RawRows[F] = new RawRows(project, database, table)
}
