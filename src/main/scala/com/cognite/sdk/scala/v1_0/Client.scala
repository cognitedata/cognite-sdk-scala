package com.cognite.sdk.scala.v1_0

import com.cognite.sdk.scala.common.{Auth, Login}
import com.softwaremill.sttp._

final class Client[F[_]](implicit auth: Auth, sttpBackend: SttpBackend[F, _]) {
  // TODO: auth once here instead of passing Auth down
  //val sttp1 = sttp.auth(auth)

  val login = new Login()
  val assets = new Assets()
  val events = new Events()
  val files = new Files()
  val timeSeries = new TimeSeriesResourceRead()
  val dataPoints = new DataPointsResourceV1()
  val rawDatabases = new RawDatabases()
  def rawTables(database: String): RawTables[F] = new RawTables(database)
  def rawRows(database: String, table: String): RawRows[F] = new RawRows(database, table)
}
