package com.cognite.sdk.scala.v1

import com.cognite.sdk.scala.common.{Auth, InvalidAuthentication, Login}
import com.cognite.sdk.scala.v1.resources.{
  Assets,
  DataPointsResourceV1,
  Events,
  Files,
  RawDatabases,
  RawRows,
  RawTables,
  ThreeDAssetMappings,
  ThreeDModels,
  ThreeDRevisions,
  TimeSeriesResource
}
import com.softwaremill.sttp._

import scala.concurrent.duration._

class GenericClient[F[_], _](implicit auth: Auth, sttpBackend: SttpBackend[F, _]) {
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
  val dataPoints = new DataPointsResourceV1(project)

  val rawDatabases = new RawDatabases(project)
  def rawTables(database: String): RawTables[F] = new RawTables(project, database)
  def rawRows(database: String, table: String): RawRows[F] = new RawRows(project, database, table)

  val threeDModels = new ThreeDModels(project)
  def threeDRevisions(modelId: Long): ThreeDRevisions[F] = new ThreeDRevisions(project, modelId)
  def threeDAssetMappings(modelId: Long, revisionId: Long): ThreeDAssetMappings[F] =
    new ThreeDAssetMappings(project, modelId, revisionId)
}

final case class Client()(implicit sttpBackend: SttpBackend[Id, Nothing])
    extends GenericClient[Id, Nothing]()
