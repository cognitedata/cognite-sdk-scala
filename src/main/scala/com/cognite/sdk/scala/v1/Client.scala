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
import scala.util.control.NonFatal

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

  private val sttpRequest = sttp
    .followRedirects(false)
    .auth(auth)
    .header("x-cdp-sdk", s"${BuildInfo.organization}-${BuildInfo.version}")
    .header("x-cdp-app", applicationName)
    .readTimeout(90.seconds)
    .parseResponseIfMetadata(_.contentLength.forall(_ > 0))

  private def parseResponse[T, R](uri: Uri, mapResult: T => R)(
      implicit decoder: Decoder[Either[CdpApiError, T]]
  ) =
    asJson[Either[CdpApiError, T]].mapWithMetadata(
      (response, metadata) =>
        response match {
          case Left(value) => throw value.error
          case Right(Left(cdpApiError)) =>
            throw cdpApiError.asException(uri"$uri", metadata.header("x-request-id"))
          case Right(Right(value)) => mapResult(value)
        }
    )

  private def unsafeBody[R](uri: Uri, response: Response[R]): R =
    try {
      response.unsafeBody
    } catch {
      // sttp's .unsafeBody returns a NoSuchElementException if the status wasn't
      // a 200 and there is no body to return.
      case _: NoSuchElementException =>
        val code = response.code.toString
        throw SdkException(
          s"Unexpected status code $code",
          Some(uri),
          response.header("x-request-id")
        )
      case NonFatal(_) =>
        throw SdkException(
          s"Unexpected exception while reading response body",
          Some(uri),
          response.header("x-request-id")
        )
    }

  def get[R, T](
      uri: Uri,
      mapResult: T => R,
      contentType: String = "application/json",
      accept: String = "application/json"
  )(implicit decoder: Decoder[Either[CdpApiError, T]]): F[R] =
    sttpRequest
      .contentType(contentType)
      .header("accept", accept)
      .get(uri)
      .response(parseResponse(uri, mapResult))
      .send()(sttpBackend, implicitly)
      .map(unsafeBody(uri, _))

  def post[R, T, I](
      body: I,
      uri: Uri,
      mapResult: T => R,
      contentType: String = "application/json",
      accept: String = "application/json"
  )(implicit serializer: BodySerializer[I], decoder: Decoder[Either[CdpApiError, T]]): F[R] =
    sttpRequest
      .contentType(contentType)
      .header("accept", accept)
      .post(uri)
      .body(body)
      .response(parseResponse(uri, mapResult))
      .send()(sttpBackend, implicitly)
      .map(unsafeBody(uri, _))

  def sendCdf[R](
      r: RequestT[Empty, String, Nothing] => RequestT[Id, R, Nothing],
      contentType: String = "application/json",
      accept: String = "application/json"
  ): F[R] = {
    val request = r(
      sttpRequest
        .contentType(contentType)
        .header("accept", accept)
    )
    request.send()(sttpBackend, implicitly).map(unsafeBody(request.uri, _))
  }

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

  lazy val projectName: String = auth.project.getOrElse {
    val loginStatus = new Login(
      RequestSession(applicationName, uri, sttpBackend, auth)
    ).status().extract
    if (loginStatus.project.trim.isEmpty) {
      throw InvalidAuthentication()
    } else {
      loginStatus.project
    }
  }

  lazy val requestSession =
    RequestSession(
      applicationName,
      uri"$uri/api/v1/projects/$projectName",
      sttpBackend,
      auth
    )
  lazy val login = new Login[F](RequestSession(applicationName, uri, sttpBackend, auth))
  lazy val assets = new Assets[F](requestSession)
  lazy val events = new Events[F](requestSession)
  lazy val files = new Files[F](requestSession)
  lazy val timeSeries = new TimeSeriesResource[F](requestSession)
  lazy val dataPoints = new DataPointsResource[F](requestSession)
  lazy val sequences = new SequencesResource[F](requestSession)
  lazy val sequenceRows = new SequenceRows[F](requestSession)

  lazy val rawDatabases = new RawDatabases[F](requestSession)
  def rawTables(database: String): RawTables[F] = new RawTables(requestSession, database)
  def rawRows(database: String, table: String): RawRows[F] =
    new RawRows(requestSession, database, table)

  lazy val threeDModels = new ThreeDModels[F](requestSession)
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
  lazy val serviceAccounts = new ServiceAccounts[F](requestSession)
  lazy val apiKeys = new ApiKeys[F](requestSession)
  lazy val groups = new Groups[F](requestSession)
  lazy val securityCategories = new SecurityCategories[F](requestSession)
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
