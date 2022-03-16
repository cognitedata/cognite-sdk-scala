// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1

import BuildInfo.BuildInfo
import cats.{Id, Monad}
import cats.implicits._
import com.cognite.sdk.scala.common._
import com.cognite.sdk.scala.v1.GenericClient.parseResponse
import com.cognite.sdk.scala.v1.resources._
import sttp.client3._
import sttp.client3.circe.asJsonEither
import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder
import sttp.capabilities.Effect
import sttp.model.Uri
import sttp.monad.MonadError

import java.net.{InetAddress, UnknownHostException}
import scala.concurrent.duration._
import scala.util.control.NonFatal

class AuthSttpBackend[F[_], +P](delegate: SttpBackend[F, P], authProvider: AuthProvider[F])
    extends SttpBackend[F, P] {
  override def send[T, R >: P with Effect[F]](request: Request[T, R]): F[Response[T]] =
    responseMonad.flatMap(authProvider.getAuth) { (auth: Auth) =>
      delegate.send(auth.auth(request))
    }

  override def close(): F[Unit] = delegate.close()

  override def responseMonad: MonadError[F] = delegate.responseMonad
}

final case class RequestSession[F[_]: Monad](
    applicationName: String,
    baseUrl: Uri,
    baseSttpBackend: SttpBackend[F, _],
    auth: AuthProvider[F],
    clientTag: Option[String] = None,
    cdfVersion: Option[String] = None
) {
  val sttpBackend: SttpBackend[F, _] = new AuthSttpBackend(baseSttpBackend, auth)

  def send[R](
      r: RequestT[Empty, Either[String, String], Any] => RequestT[Id, R, Any]
  ): F[Response[R]] =
    r(emptyRequest.readTimeout(90.seconds)).send(sttpBackend)

  private val sttpRequest = {
    val baseRequest = basicRequest
      .followRedirects(false)
      .header("x-cdp-sdk", s"CogniteScalaSDK:${BuildInfo.version}")
      .header("x-cdp-app", applicationName)
      .readTimeout(90.seconds)
    (clientTag, cdfVersion) match {
      case (Some(tag), Some(ver)) =>
        baseRequest.header("x-cdp-clienttag", tag).header("cdf-version", ver)
      case (Some(tag), None) => baseRequest.header("x-cdp-clienttag", tag)
      case (None, Some(ver)) => baseRequest.header("cdf-version", ver)
      case (None, None) => baseRequest
    }
  }

  def get[R, T](
      uri: Uri,
      mapResult: T => R,
      contentType: String = "application/json",
      accept: String = "application/json"
  )(implicit decoder: Decoder[T]): F[R] =
    sttpRequest
      .contentType(contentType)
      .header("accept", accept)
      .get(uri)
      .response(parseResponse(uri, mapResult))
      .send(sttpBackend)
      .map(_.body)

  def postEmptyBody[R, T](
      uri: Uri,
      mapResult: T => R,
      contentType: String = "application/json",
      accept: String = "application/json"
  )(implicit decoder: Decoder[T]): F[R] =
    sttpRequest
      .contentType(contentType)
      .header("accept", accept)
      .header("cdf-version", "alpha")
      .post(uri)
      .response(parseResponse(uri, mapResult))
      .send(sttpBackend)
      .map(_.body)

  def post[R, T, I](
      body: I,
      uri: Uri,
      mapResult: T => R,
      contentType: String = "application/json",
      accept: String = "application/json"
  )(implicit serializer: BodySerializer[I], decoder: Decoder[T]): F[R] =
    sttpRequest
      .contentType(contentType)
      .header("accept", accept)
      .post(uri)
      .body(body)
      .response(parseResponse(uri, mapResult))
      .send(sttpBackend)
      .map(_.body)

  def sendCdf[R](
      r: RequestT[Empty, Either[String, String], Any] => RequestT[Id, R, Any],
      contentType: String = "application/json",
      accept: String = "application/json"
  ): F[R] =
    r(
      sttpRequest
        .contentType(contentType)
        .header("accept", accept)
    )
      .send(sttpBackend)
      .map(_.body)

  def map[R, R1](r: F[R], f: R => R1): F[R1] = r.map(f)
  def flatMap[R, R1](r: F[R], f: R => F[R1]): F[R1] = r.flatMap(f)
}

// scalastyle:off parameter.number
class GenericClient[F[_]](
    applicationName: String,
    val projectName: String,
    baseUrl: String,
    authProvider: AuthProvider[F],
    apiVersion: Option[String],
    clientTag: Option[String],
    cdfVersion: Option[String]
)(implicit monad: Monad[F], sttpBackend: SttpBackend[F, Any]) {
  def this(
      applicationName: String,
      projectName: String,
      baseUrl: String = GenericClient.defaultBaseUrl,
      auth: Auth = Auth.defaultAuth,
      apiVersion: Option[String] = None,
      clientTag: Option[String] = None,
      cdfVersion: Option[String] = None
  )(implicit monad: Monad[F], sttpBackend: SttpBackend[F, Any]) =
    this(
      applicationName,
      projectName,
      baseUrl,
      AuthProvider[F](auth),
      apiVersion,
      clientTag,
      cdfVersion
    )
  // scalastyle:on parameter.number

  import GenericClient._

  val uri: Uri = parseBaseUrlOrThrow(baseUrl)

  lazy val requestSession: RequestSession[F] =
    RequestSession(
      applicationName,
      uri"$uri/api/${apiVersion.getOrElse("v1")}/projects/$projectName",
      sttpBackend,
      authProvider,
      clientTag,
      cdfVersion
    )
  lazy val login =
    new Login[F](RequestSession(applicationName, uri, sttpBackend, authProvider, clientTag))
  lazy val token =
    new Token[F](RequestSession(applicationName, uri, sttpBackend, authProvider, clientTag))
  lazy val assets = new Assets[F](requestSession)
  lazy val events = new Events[F](requestSession)
  lazy val files = new Files[F](requestSession)
  lazy val timeSeries = new TimeSeriesResource[F](requestSession)
  lazy val dataPoints = new DataPointsResource[F](requestSession)
  lazy val sequences = new SequencesResource[F](requestSession)
  lazy val sequenceRows = new SequenceRows[F](requestSession)
  lazy val dataSets = new DataSets[F](requestSession)
  lazy val labels = new Labels[F](requestSession)
  lazy val relationships = new Relationships[F](requestSession)

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

  lazy val functions = new Functions[F](requestSession)
  def functionCalls(functionId: Long): FunctionCalls[F] =
    new FunctionCalls(requestSession, functionId)
  lazy val functionSchedules = new FunctionSchedules[F](requestSession)

  lazy val sessions = new Sessions[F](requestSession)
  lazy val dataModels = new DataModels[F](requestSession)
  lazy val dataModelInstances = new DataModelInstances[F](requestSession, dataModels)

  def project: F[Project] =
    requestSession.get[Project, Project](
      requestSession.baseUrl,
      value => value
    )
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

  val defaultBaseUrl: String = Option(System.getenv("COGNITE_BASE_URL"))
    .getOrElse("https://api.cognitedata.com")

  def apply[F[_]: Monad](applicationName: String, projectName: String, baseUrl: String, auth: Auth)(
      implicit sttpBackend: SttpBackend[F, Any]
  ): GenericClient[F] =
    new GenericClient(applicationName, projectName, baseUrl, auth)(implicitly, sttpBackend)

  def parseBaseUrlOrThrow(baseUrl: String): Uri =
    try {
      // sttp allows this, but we don't.
      if (baseUrl.isEmpty) {
        throw new IllegalArgumentException()
      }
      val uri = uri"$baseUrl"
      val uriWithScheme = if (uri.scheme.isEmpty) {
        uri"https://$baseUrl"
      } else {
        uri
      }
      uriWithScheme.host match {
        case Some(host) =>
          // Validate that this is a valid hostname.
          val _ = InetAddress.getByName(host)
          uriWithScheme
        case None => throw new UnknownHostException(s"Unknown host in baseUrl: $baseUrl")
      }
    } catch {
      case e: UnknownHostException => throw e
      case NonFatal(_) =>
        throw new IllegalArgumentException(
          s"Unable to parse this baseUrl as a valid URI: $baseUrl"
        )
    }

  def forAuth[F[_]: Monad](
      applicationName: String,
      auth: Auth,
      baseUrl: String = defaultBaseUrl,
      apiVersion: Option[String] = None,
      clientTag: Option[String] = None,
      cdfVersion: Option[String] = None
  )(implicit sttpBackend: SttpBackend[F, Any]): F[GenericClient[F]] =
    forAuthProvider(applicationName, AuthProvider(auth), baseUrl, apiVersion, clientTag, cdfVersion)

  def forAuthProvider[F[_]: Monad](
      applicationName: String,
      authProvider: AuthProvider[F],
      baseUrl: String = defaultBaseUrl,
      apiVersion: Option[String] = None,
      clientTag: Option[String] = None,
      cdfVersion: Option[String] = None
  )(implicit sttpBackend: SttpBackend[F, Any]): F[GenericClient[F]] = {
    val login = new Login[F](
      RequestSession(applicationName, parseBaseUrlOrThrow(baseUrl), sttpBackend, authProvider)
    )

    for {
      status <- login.status()
      projectName = status.project
    } yield
      if (projectName.trim.isEmpty) {
        throw InvalidAuthentication()
      } else {
        new GenericClient[F](
          applicationName,
          projectName,
          baseUrl,
          authProvider,
          apiVersion,
          clientTag,
          cdfVersion
        )(
          implicitly,
          sttpBackend
        )
      }
  }

  def parseResponse[T, R](uri: Uri, mapResult: T => R)(
      implicit decoder: Decoder[T]
  ): ResponseAs[R, Any] =
    asJsonEither[CdpApiError, T].mapWithMetadata((response, metadata) =>
      response match {
        case Left(DeserializationException(_, error)) =>
          throw SdkException(
            s"Failed to parse response, reason: ${error.getMessage}",
            Some(uri),
            metadata.header("x-request-id"),
            Some(metadata.code.code)
          )
        case Left(HttpError(cdpApiError, _)) =>
          throw cdpApiError.asException(uri"$uri", metadata.header("x-request-id"))
        case Right(value) => mapResult(value)
      }
    )
}

class Client(
    applicationName: String,
    override val projectName: String,
    baseUrl: String =
      Option(System.getenv("COGNITE_BASE_URL")).getOrElse("https://api.cognitedata.com"),
    auth: Auth = Auth.defaultAuth
)(implicit sttpBackend: SttpBackend[Id, Any])
    extends GenericClient[Id](applicationName, projectName, baseUrl, auth)

object Client {
  def apply(applicationName: String, projectName: String, baseUrl: String, auth: Auth)(
      implicit sttpBackend: SttpBackend[Id, Any]
  ): Client = new Client(applicationName, projectName, baseUrl, auth)(sttpBackend)
}
