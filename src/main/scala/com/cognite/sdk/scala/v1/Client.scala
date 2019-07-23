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

final case class RequestSession[F[_]](baseUri: Uri, sttpBackend: SttpBackend[F, _], auth: Auth) {
  def send[R](r: RequestT[Empty, String, Nothing] => RequestT[Id, R, Nothing]): F[Response[R]] =
    r(sttp
      .auth(auth)
      .contentType("application/json")
      .header("accept", "application/json")
      .readTimeout(90.seconds)
      .parseResponseIf(_ => true))
      .send()(sttpBackend, implicitly)
}

class GenericClient[F[_], _](implicit auth: Auth, sttpBackend: SttpBackend[F, _]) {
  val project: String = auth.project.getOrElse {
    val sttpBackend: SttpBackend[Id, Nothing] = HttpURLConnectionBackend(
      options = SttpBackendOptions.connectionTimeout(90.seconds)
    )

    val loginStatus = new Login(RequestSession(uri"https://api.cognitedata.com", sttpBackend, auth)).status().unsafeBody

    if (loginStatus.project.trim.isEmpty) {
      throw InvalidAuthentication()
    } else {
      loginStatus.project
    }
  }

  val requestSession = RequestSession(uri"https://api.cognitedata.com/api/v1/projects/$project",
    sttpBackend, auth)
  val login = new Login[F](requestSession.copy(baseUri = uri"https://api.cognitedata.com"))
  val assets = new Assets[F](requestSession)
  val events = new Events[F](requestSession)
  val files = new Files[F](requestSession)
  val timeSeries = new TimeSeriesResource[F](requestSession)
  val dataPoints = new DataPointsResourceV1[F](requestSession)

  val rawDatabases = new RawDatabases[F](requestSession)
  def rawTables(database: String): RawTables[F] = new RawTables(requestSession, database)
  def rawRows(database: String, table: String): RawRows[F] = new RawRows(requestSession, database, table)

  val threeDModels = new ThreeDModels[F](requestSession)
  def threeDRevisions(modelId: Long): ThreeDRevisions[F] = new ThreeDRevisions(requestSession, modelId)
  def threeDAssetMappings(modelId: Long, revisionId: Long): ThreeDAssetMappings[F] =
    new ThreeDAssetMappings(requestSession, modelId, revisionId)
}

final case class Client()(implicit auth: Auth, sttpBackend: SttpBackend[Id, Nothing])
    extends GenericClient[Id, Nothing]()
