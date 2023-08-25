// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1

import com.cognite.scala_sdk.BuildInfo
import cats.implicits._
import cats.{Id, Monad}
import com.cognite.sdk.scala.common._
import com.cognite.sdk.scala.v1.GenericClient.parseResponse
import com.cognite.sdk.scala.v1.resources._
import com.cognite.sdk.scala.v1.resources.fdm.datamodels.{DataModels => DataModelsV3}
import com.cognite.sdk.scala.v1.resources.fdm.containers.Containers
import com.cognite.sdk.scala.v1.resources.fdm.instances.Instances
import com.cognite.sdk.scala.v1.resources.fdm.views.Views
import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder
import sttp.capabilities.Effect
import sttp.client3._
import sttp.client3.circe.asJsonEither
import sttp.model.{Header, StatusCode, Uri}
import sttp.monad.MonadError

import java.net.{InetAddress, UnknownHostException}
import scala.concurrent.duration._
import scala.util.control.NonFatal
import natchez.Trace

class TraceSttpBackend[F[_]: Trace, +P](delegate: SttpBackend[F, P]) extends SttpBackend[F, P] {

  def sendImpl[T, R >: P with Effect[F]](
      request: Request[T, R]
  )(implicit monad: MonadError[F]): F[Response[T]] =
    Trace[F].span("sttp-client-request") {
      import sttp.monad.syntax._
      for {
        knl <- Trace[F].kernel
        _ <- Trace[F].put(
          "client.http.uri" -> request.uri.toString(),
          "client.http.method" -> request.method.toString
        )
        response <- delegate.send(
          request.headers(
            knl.toHeaders.map { case (k, v) => Header(k.toString, v) }.toSeq: _*
          )
        )
        _ <- Trace[F].put("client.http.status_code" -> response.code.toString())
      } yield response
    }
  override def send[T, R >: P with Effect[F]](request: Request[T, R]): F[Response[T]] =
    sendImpl(request)(responseMonad)

  override def close(): F[Unit] = delegate.close()

  override def responseMonad: MonadError[F] = delegate.responseMonad
}

class AuthSttpBackend[F[_], +P](delegate: SttpBackend[F, P], authProvider: AuthProvider[F])
    extends SttpBackend[F, P] {
  override def send[T, R >: P with Effect[F]](request: Request[T, R]): F[Response[T]] =
    responseMonad.flatMap(authProvider.getAuth) { (auth: Auth) =>
      delegate.send(auth.auth(request))
    }

  override def close(): F[Unit] = delegate.close()

  override def responseMonad: MonadError[F] = delegate.responseMonad
}

final case class RequestSession[F[_]: Monad: Trace](
    applicationName: String,
    baseUrl: Uri,
    baseSttpBackend: SttpBackend[F, _],
    auth: AuthProvider[F],
    clientTag: Option[String] = None,
    cdfVersion: Option[String] = None,
    tags: Map[String, Any] = Map.empty
) {
  def withResourceType(resourceType: GenericClient.RESOURCE_TYPE): RequestSession[F] =
    this.copy(tags = this.tags + (GenericClient.RESOURCE_TYPE_TAG -> resourceType))

  val sttpBackend: SttpBackend[F, _] =
    new AuthSttpBackend(new TraceSttpBackend(baseSttpBackend), auth)

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
      .headers(
        Seq(
          clientTag.map(Header("x-cdp-clienttag", _)),
          cdfVersion.map(Header("cdf-version", _))
        ).flatMap(_.toList): _*
      )
    tags.foldLeft(baseRequest)((req, tag) => req.tag(tag._1, tag._2))
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
class GenericClient[F[_]: Trace](
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
      auth: Auth,
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
  lazy val token =
    new Token[F](RequestSession(applicationName, uri, sttpBackend, authProvider, clientTag))
  lazy val assets = new Assets[F](requestSession.withResourceType(ASSETS))
  lazy val events = new Events[F](requestSession.withResourceType(EVENTS))
  lazy val files = new Files[F](requestSession.withResourceType(FILES))
  lazy val timeSeries =
    new TimeSeriesResource[F](requestSession.withResourceType(TIMESERIES))
  lazy val dataPoints =
    new DataPointsResource[F](requestSession.withResourceType(DATAPOINTS))
  lazy val sequences =
    new SequencesResource[F](requestSession.withResourceType(SEQUENCES))
  lazy val sequenceRows =
    new SequenceRows[F](requestSession.withResourceType(SEQUENCES_ROWS))
  lazy val dataSets = new DataSets[F](requestSession.withResourceType(DATASETS))
  lazy val labels = new Labels[F](requestSession.withResourceType(LABELS))
  lazy val relationships =
    new Relationships[F](requestSession.withResourceType(RELATIONSHIPS))

  lazy val rawDatabases =
    new RawDatabases[F](requestSession.withResourceType(RAW_METADATA))
  def rawTables(database: String): RawTables[F] =
    new RawTables(requestSession.withResourceType(RAW_METADATA), database)
  def rawRows(database: String, table: String): RawRows[F] =
    new RawRows(requestSession.withResourceType(RAW_ROWS), database, table)

  lazy val threeDModels = new ThreeDModels[F](requestSession.withResourceType(THREED))
  def threeDRevisions(modelId: Long): ThreeDRevisions[F] =
    new ThreeDRevisions(requestSession.withResourceType(THREED), modelId)
  def threeDAssetMappings(modelId: Long, revisionId: Long): ThreeDAssetMappings[F] =
    new ThreeDAssetMappings(
      requestSession.withResourceType(THREED),
      modelId,
      revisionId
    )
  def threeDNodes(modelId: Long, revisionId: Long): ThreeDNodes[F] =
    new ThreeDNodes(requestSession.withResourceType(THREED), modelId, revisionId)

  lazy val functions = new Functions[F](requestSession.withResourceType(FUNCTIONS))
  def functionCalls(functionId: Long): FunctionCalls[F] =
    new FunctionCalls(requestSession.withResourceType(FUNCTIONS), functionId)
  lazy val functionSchedules =
    new FunctionSchedules[F](requestSession.withResourceType(FUNCTIONS))

  lazy val sessions = new Sessions[F](requestSession.withResourceType(SESSIONS))
  lazy val transformations =
    new Transformations[F](requestSession.withResourceType(TRANSFORMATIONS))
  @deprecated("message", since = "0")
  lazy val dataModels =
    new DataModels[F](requestSession.withResourceType(OLD_DATAMODELS))
  @deprecated("message", since = "0")
  lazy val nodes =
    new Nodes[F](requestSession.withResourceType(OLD_DATAMODELS), dataModels)
  @deprecated("message", since = "0")
  lazy val spaces = new Spaces[F](requestSession.withResourceType(OLD_DATAMODELS))
  @deprecated("message", since = "0")
  lazy val edges =
    new Edges[F](requestSession.withResourceType(OLD_DATAMODELS), dataModels)
  @deprecated("message", since = "0")
  lazy val containers =
    new Containers[F](requestSession.withResourceType(OLD_DATAMODELS))
  @deprecated("message", since = "0")
  lazy val instances =
    new Instances[F](requestSession.withResourceType(OLD_DATAMODELS))
  @deprecated("message", since = "0")
  lazy val views = new Views[F](requestSession.withResourceType(OLD_DATAMODELS))
  lazy val spacesv3 = new SpacesV3[F](requestSession.withResourceType(DATAMODELS))
  lazy val dataModelsV3 =
    new DataModelsV3[F](requestSession.withResourceType(DATAMODELS))

  lazy val wdl = new WellDataLayer[F](
    RequestSession(
      applicationName,
      uri"$uri/api/v1/projects/$projectName",
      sttpBackend,
      authProvider,
      clientTag,
      Some("20221206-beta")
    ).withResourceType(WELLS)
  )

  def project: F[Project] =
    requestSession
      .withResourceType(PROJECT)
      .get[Project, Project](
        requestSession.baseUrl,
        value => value
      )
  lazy val groups = new Groups[F](requestSession.withResourceType(GROUPS))
  lazy val securityCategories =
    new SecurityCategories[F](requestSession.withResourceType(SECURITY_CATEGORIES))
}

object GenericClient {
  val RESOURCE_TYPE_TAG = "cdf-resource-type"

  sealed trait RESOURCE_TYPE
  case object ASSETS extends RESOURCE_TYPE
  case object EVENTS extends RESOURCE_TYPE
  case object FILES extends RESOURCE_TYPE
  case object TIMESERIES extends RESOURCE_TYPE
  case object DATAPOINTS extends RESOURCE_TYPE
  case object SEQUENCES extends RESOURCE_TYPE
  case object SEQUENCES_ROWS extends RESOURCE_TYPE
  case object DATASETS extends RESOURCE_TYPE
  case object LABELS extends RESOURCE_TYPE
  case object RELATIONSHIPS extends RESOURCE_TYPE
  case object RAW_METADATA extends RESOURCE_TYPE
  case object RAW_ROWS extends RESOURCE_TYPE
  case object THREED extends RESOURCE_TYPE
  case object FUNCTIONS extends RESOURCE_TYPE
  case object SESSIONS extends RESOURCE_TYPE
  case object TRANSFORMATIONS extends RESOURCE_TYPE
  @deprecated("message", since = "0")
  case object OLD_DATAMODELS extends RESOURCE_TYPE
  case object DATAMODELS extends RESOURCE_TYPE
  case object WELLS extends RESOURCE_TYPE
  case object PROJECT extends RESOURCE_TYPE
  case object GROUPS extends RESOURCE_TYPE
  case object SECURITY_CATEGORIES extends RESOURCE_TYPE

  implicit val projectAuthenticationDecoder: Decoder[ProjectAuthentication] =
    deriveDecoder[ProjectAuthentication]
  @SuppressWarnings(Array("org.wartremover.warts.JavaSerializable"))
  implicit val projectDecoder: Decoder[Project] = deriveDecoder[Project]

  val defaultBaseUrl: String = Option(System.getenv("COGNITE_BASE_URL"))
    .getOrElse("https://api.cognitedata.com")

  def apply[F[_]: Monad: Trace](
      applicationName: String,
      projectName: String,
      baseUrl: String,
      auth: Auth
  )(implicit sttpBackend: SttpBackend[F, Any]): GenericClient[F] =
    new GenericClient(applicationName, projectName, baseUrl, auth)

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

  def forAuth[F[_]: Monad: Trace](
      applicationName: String,
      projectName: String,
      auth: Auth,
      baseUrl: String = defaultBaseUrl,
      apiVersion: Option[String] = None,
      clientTag: Option[String] = None,
      cdfVersion: Option[String] = None
  )(implicit sttpBackend: SttpBackend[F, Any]): F[GenericClient[F]] =
    forAuthProvider(
      applicationName,
      projectName,
      AuthProvider(auth),
      baseUrl,
      apiVersion,
      clientTag,
      cdfVersion
    )

  def forAuthProvider[F[_]: Monad: Trace](
      applicationName: String,
      projectName: String,
      authProvider: AuthProvider[F],
      baseUrl: String = defaultBaseUrl,
      apiVersion: Option[String] = None,
      clientTag: Option[String] = None,
      cdfVersion: Option[String] = None
  )(implicit sttpBackend: SttpBackend[F, Any]): F[GenericClient[F]] =
    if (projectName.isEmpty) {
      throw InvalidAuthentication()
    } else {
      Monad[F].pure(
        new GenericClient[F](
          applicationName,
          projectName,
          baseUrl,
          authProvider,
          apiVersion,
          clientTag,
          cdfVersion
        )
      )
    }

  def parseResponse[T, R](uri: Uri, mapResult: T => R)(
      implicit decoder: Decoder[T]
  ): ResponseAs[R, Any] =
    asJsonEither[CdpApiError, T].mapWithMetadata((response, metadata) =>
      response match {
        case Left(DeserializationException(_, _))
            if metadata.code.code === StatusCode.TooManyRequests.code =>
          throw CdpApiException(
            url = uri"$uri",
            code = StatusCode.TooManyRequests.code,
            missing = None,
            duplicated = None,
            missingFields = None,
            message = "Too many requests.",
            requestId = metadata.header("x-request-id")
          )
        case Left(DeserializationException(_, error)) =>
          throw SdkException(
            s"Failed to parse response, reason: ${error.getMessage}",
            Some(uri),
            metadata.header("x-request-id"),
            Some(metadata.code.code)
          )
        case Left(HttpError(cdpApiError, _)) =>
          throw cdpApiError.asException(uri"$uri", metadata.header("x-request-id"))
        case Right(value) =>
          mapResult(value)
      }
    )
}

class Client(
    applicationName: String,
    override val projectName: String,
    baseUrl: String =
      Option(System.getenv("COGNITE_BASE_URL")).getOrElse("https://api.cognitedata.com"),
    auth: Auth
)(implicit trace: Trace[Id], sttpBackend: SttpBackend[Id, Any])
    extends GenericClient[Id](applicationName, projectName, baseUrl, auth)

object Client {
  def apply(
      applicationName: String,
      projectName: String,
      baseUrl: String,
      auth: Auth
  )(
      implicit trace: Trace[Id],
      sttpBackend: SttpBackend[Id, Any]
  ): Client = new Client(applicationName, projectName, baseUrl, auth)
}
