package com.cognite.sdk.scala.v1

import BuildInfo.BuildInfo
import cats.Monad
import cats.implicits._
import com.cognite.sdk.scala.common.{Auth, InvalidAuthentication, Login}
import com.cognite.sdk.scala.v1.resources.{
  Assets,
  DataPointsResourceV1,
  Events,
  Files,
  RawDatabases,
  RawRows,
  RawTables,
  SequenceRows,
  SequencesResource,
  ThreeDAssetMappings,
  ThreeDModels,
  ThreeDRevisions,
  TimeSeriesResource
}
import com.softwaremill.sttp._

import scala.concurrent.duration._

final case class RequestSession[F[_]: Monad](
    applicationName: String,
    baseUri: Uri,
    sttpBackend: SttpBackend[F, _],
    auth: Auth
) {
  def send[R](r: RequestT[Empty, String, Nothing] => RequestT[Id, R, Nothing]): F[Response[R]] =
    r(
      sttp
        .readTimeout(90.seconds)
    ).send()(sttpBackend, implicitly)

  def sendCdf[R](
      r: RequestT[Empty, String, Nothing] => RequestT[Id, R, Nothing],
      contentType: String = "application/json",
      accept: String = "application/json"
  ): F[R] =
    r(
      sttp
        .followRedirects(false)
        .auth(auth)
        .contentType(contentType)
        .header("accept", accept)
        .header("x-cdp-sdk", s"${BuildInfo.organization}-${BuildInfo.version}")
        .header("x-cdp-app", applicationName)
        .readTimeout(90.seconds)
        .parseResponseIf(_ => true)
    ).send()(sttpBackend, implicitly).map(_.unsafeBody)

  def map[R, R1](r: F[R], f: R => R1): F[R1] = r.map(f)
  def flatMap[R, R1](r: F[R], f: R => F[R1]): F[R1] = r.flatMap(f)
}

class GenericClient[F[_]: Monad, _](
    applicationName: String,
    baseUri: String =
      Option(System.getenv("COGNITE_BASE_URL")).getOrElse("https://api.cognitedata.com")
)(
    implicit auth: Auth,
    sttpBackend: SttpBackend[F, _]
) {

  val project: String = auth.project.getOrElse {
    val sttpBackend: SttpBackend[Id, Nothing] = HttpURLConnectionBackend(
      options = SttpBackendOptions.connectionTimeout(90.seconds)
    )
    val loginStatus = new Login(
      RequestSession(applicationName, uri"$baseUri", sttpBackend, auth)
    ).status()

    if (loginStatus.project.trim.isEmpty) {
      throw InvalidAuthentication()
    } else {
      loginStatus.project
    }
  }

  val requestSession =
    RequestSession(
      applicationName,
      uri"$baseUri/api/v1/projects/$project",
      sttpBackend,
      auth
    )
  val login = new Login[F](requestSession.copy(baseUri = uri"$baseUri"))
  val assets = new Assets[F](requestSession)
  val events = new Events[F](requestSession)
  val files = new Files[F](requestSession)
  val timeSeries = new TimeSeriesResource[F](requestSession)
  val dataPoints = new DataPointsResourceV1[F](requestSession)
  val sequences = new SequencesResource[F](requestSession)
  val sequenceRows = new SequenceRows[F](requestSession)

  val rawDatabases = new RawDatabases[F](requestSession)
  def rawTables(database: String): RawTables[F] = new RawTables(requestSession, database)
  def rawRows(database: String, table: String): RawRows[F] =
    new RawRows(requestSession, database, table)

  val threeDModels = new ThreeDModels[F](requestSession)
  def threeDRevisions(modelId: Long): ThreeDRevisions[F] =
    new ThreeDRevisions(requestSession, modelId)
  def threeDAssetMappings(modelId: Long, revisionId: Long): ThreeDAssetMappings[F] =
    new ThreeDAssetMappings(requestSession, modelId, revisionId)
}

final case class Client(
    applicationName: String,
    baseUri: String =
      Option(System.getenv("COGNITE_BASE_URL")).getOrElse("https://api.cognitedata.com")
)(
    implicit auth: Auth,
    sttpBackend: SttpBackend[Id, Nothing]
) extends GenericClient[Id, Nothing](applicationName, baseUri)
