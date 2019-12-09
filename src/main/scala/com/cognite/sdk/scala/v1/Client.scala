package com.cognite.sdk.scala.v1

import BuildInfo.BuildInfo
import cats.{Comonad, Id, Monad}
import cats.implicits._
import com.cognite.sdk.scala.common._
import com.cognite.sdk.scala.v1.resources._
import com.softwaremill.sttp.SttpBackend
import com.softwaremill.sttp.{Id => _, _}
import com.softwaremill.sttp.circe.asJson
import io.circe.Decoder
import io.circe.derivation.deriveDecoder

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

  def get[R, T](
      uri: Uri,
      mapResult: T => R,
      contentType: String = "application/json",
      accept: String = "application/json"
  )(implicit decoder: Decoder[Either[CdpApiError, T]]): F[R] =
    sttp
      .followRedirects(false)
      .auth(auth)
      .contentType(contentType)
      .header("accept", accept)
      .header("x-cdp-sdk", s"${BuildInfo.organization}-${BuildInfo.version}")
      .header("x-cdp-app", applicationName)
      .readTimeout(90.seconds)
      .parseResponseIf(_ => true)
      .get(uri)
      .response(
        asJson[Either[CdpApiError, T]].mapWithMetadata(
          (response, metadata) =>
            response match {
              case Left(value) => throw value.error
              case Right(Left(cdpApiError)) =>
                throw cdpApiError.asException(uri"$uri", metadata.header("x-request-id"))
              case Right(Right(value)) => mapResult(value)
            }
        )
      )
      .send()(sttpBackend, implicitly)
      .map(_.unsafeBody)

  def post[R, T, I](
      body: I,
      uri: Uri,
      mapResult: T => R,
      contentType: String = "application/json",
      accept: String = "application/json"
  )(implicit serializer: BodySerializer[I], decoder: Decoder[Either[CdpApiError, T]]): F[R] =
    sttp
      .followRedirects(false)
      .auth(auth)
      .contentType(contentType)
      .header("accept", accept)
      .header("x-cdp-sdk", s"${BuildInfo.organization}-${BuildInfo.version}")
      .header("x-cdp-app", applicationName)
      .readTimeout(90.seconds)
      .parseResponseIf(_ => true)
      .post(uri)
      .body(body)
      .response(
        asJson[Either[CdpApiError, T]].mapWithMetadata(
          (response, metadata) =>
            response match {
              case Left(value) => throw value.error
              case Right(Left(cdpApiError)) =>
                throw cdpApiError.asException(uri"$uri", metadata.header("x-request-id"))
              case Right(Right(value)) => mapResult(value)
            }
        )
      )
      .send()(sttpBackend, implicitly)
      .map(_.unsafeBody)

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
    ).send()(sttpBackend, implicitly).map(r => r.unsafeBody)

  def map[R, R1](r: F[R], f: R => R1): F[R1] = r.map(f)
  def flatMap[R, R1](r: F[R], f: R => F[R1]): F[R1] = r.flatMap(f)
}

class GenericClient[F[_]: Monad: Comonad, _](
    applicationName: String,
    baseUri: String =
      Option(System.getenv("COGNITE_BASE_URL")).getOrElse("https://api.cognitedata.com")
)(
    implicit auth: Auth,
    sttpBackend: SttpBackend[F, _]
) {
  import GenericClient._
  val uri: Uri = try {
    uri"$baseUri"
  } catch {
    case _: Throwable =>
      throw new IllegalArgumentException("Unable to parse URI. Please check URI syntax.")
  }

  val projectName: String = auth.project.getOrElse {
    val loginStatus = new Login(
      RequestSession(applicationName, uri"$uri", sttpBackend, auth)
    ).status().extract
    if (loginStatus.project.trim.isEmpty) {
      throw InvalidAuthentication()
    } else {
      loginStatus.project
    }
  }

  val requestSession =
    RequestSession(
      applicationName,
      uri"$uri/api/v1/projects/$projectName",
      sttpBackend,
      auth
    )
  val login = new Login[F](requestSession.copy(baseUri = uri))
  val assets = new Assets[F](requestSession)
  val events = new Events[F](requestSession)
  val files = new Files[F](requestSession)
  val timeSeries = new TimeSeriesResource[F](requestSession)
  val dataPoints = new DataPointsResource[F](requestSession)
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
  def threeDNodes(modelId: Long, revisionId: Long): ThreeDNodes[F] =
    new ThreeDNodes(requestSession, modelId, revisionId)

  def project: F[Project] = {
    implicit val errorOrItemsDecoder: Decoder[Either[CdpApiError, Project]] =
      EitherDecoder.eitherDecoder[CdpApiError, Project]
    requestSession.get[Project, Project](
      requestSession.baseUri,
      value => value
    )
  }
  val serviceAccounts = new ServiceAccounts[F](requestSession)
  val apiKeys = new ApiKeys[F](requestSession)
  val groups = new Groups[F](requestSession)
  val securityCategories = new SecurityCategories[F](requestSession)
}

object GenericClient {
  implicit val projectAuthenticationDecoder: Decoder[ProjectAuthentication] =
    deriveDecoder[ProjectAuthentication]
  @SuppressWarnings(Array("org.wartremover.warts.JavaSerializable"))
  implicit val projectDecoder: Decoder[Project] = deriveDecoder[Project]
}

final case class Client(
    applicationName: String,
    baseUri: String =
      Option(System.getenv("COGNITE_BASE_URL")).getOrElse("https://api.cognitedata.com")
)(
    implicit auth: Auth,
    sttpBackend: SttpBackend[Id, Nothing]
) extends GenericClient[Id, Nothing](applicationName, baseUri)
