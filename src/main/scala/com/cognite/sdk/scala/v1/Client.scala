package com.cognite.sdk.scala.v1

import com.cognite.sdk.scala.common.{Auth, InvalidAuthentication, Login, RequestSession}
import com.cognite.sdk.scala.v1.resources.{Assets, DataPointsResourceV1, Events, Files, RawDatabases, RawRows, RawTables, ThreeDAssetMappings, ThreeDModels, ThreeDRevisions, TimeSeriesResource}
import com.softwaremill.sttp._

import scala.concurrent.duration._

class GenericClient[F[_], _](implicit auth: Auth, sttpBackend: SttpBackend[F, _])
  extends RequestSession {
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

  val login = new Login[F]()
  val assets = new Assets[F]( this)
  val events = new Events[F](project)
  val files = new Files[F](project)
  val timeSeries = new TimeSeriesResource[F](project)
  val dataPoints = new DataPointsResourceV1[F](project)

  val rawDatabases = new RawDatabases[F](project)
  def rawTables(database: String): RawTables[F] = new RawTables(project, database)
  def rawRows(database: String, table: String): RawRows[F] = new RawRows(project, database, table)

  val threeDModels = new ThreeDModels[F](project)
  def threeDRevisions(modelId: Long): ThreeDRevisions[F] = new ThreeDRevisions(project, modelId)
  def threeDAssetMappings(modelId: Long, revisionId: Long): ThreeDAssetMappings[F] =
    new ThreeDAssetMappings(project, modelId, revisionId)

  override def request: RequestT[Empty, String, Nothing] =
    sttp
      .auth(auth)
      .contentType("application/json")
      .header("accept", "application/json")
      .readTimeout(90.seconds)
      .parseResponseIf(_ => true)

  override val baseUri: Uri = uri"https://api.cognitedata.com/api/v1/projects/$project"
}

final case class Client()(implicit auth: Auth, sttpBackend: SttpBackend[Id, Nothing])
    extends GenericClient[Id, Nothing]()
