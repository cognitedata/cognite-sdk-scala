package com.cognite.sdk.scala.v1

import cats.implicits.{catsSyntaxEq, toBifunctorOps}
import cats.{MonadError => CMonadError}
import com.cognite.sdk.scala.common.{
  Auth,
  AuthProvider,
  CdpApiError,
  CdpApiException,
  InvalidAuthentication,
  SdkException,
  Token
}
import com.cognite.sdk.scala.v1.resources.fdm.containers.Containers
import com.cognite.sdk.scala.v1.resources.fdm.datamodels.{DataModels => DataModelsV3}
import com.cognite.sdk.scala.v1.resources.fdm.instances.Instances
import com.cognite.sdk.scala.v1.resources.fdm.views.Views
import com.cognite.sdk.scala.v1.resources._
import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder
import natchez.Trace
import sttp.client3.circe.asJsonEither
import sttp.client3.{DeserializationException, HttpError, ResponseAs, SttpBackend, UriContext}
import sttp.model.{StatusCode, Uri}

import java.net.{InetAddress, UnknownHostException}
import scala.util.control.NonFatal

class GenericClient[F[_]: Trace](
    applicationName: String,
    val projectName: String,
    baseUrl: String,
    authProvider: AuthProvider[F],
    apiVersion: Option[String],
    clientTag: Option[String],
    cdfVersion: Option[String],
    sttpBackend: SttpBackend[F, Any],
    wrapSttpBackend: SttpBackend[F, Any] => SttpBackend[F, Any]
)(implicit monad: CMonadError[F, Throwable]) {
  def this(
      applicationName: String,
      projectName: String,
      baseUrl: String = GenericClient.defaultBaseUrl,
      auth: Auth,
      apiVersion: Option[String] = None,
      clientTag: Option[String] = None,
      cdfVersion: Option[String] = None,
      sttpBackend: SttpBackend[F, Any],
      wrapSttpBackend: SttpBackend[F, Any] => SttpBackend[F, Any] = identity[SttpBackend[F, Any]](_)
  )(implicit monad: CMonadError[F, Throwable]) =
    this(
      applicationName,
      projectName,
      baseUrl,
      AuthProvider[F](auth),
      apiVersion,
      clientTag,
      cdfVersion,
      sttpBackend,
      wrapSttpBackend
    )

  import GenericClient._

  val uri: Uri = parseBaseUrlOrThrow(baseUrl)

  lazy val requestSession: RequestSession[F] =
    RequestSession(
      applicationName,
      uri"$uri/api/${apiVersion.getOrElse("v1")}/projects/$projectName",
      sttpBackend,
      wrapSttpBackend,
      authProvider,
      clientTag,
      cdfVersion
    )
  lazy val token =
    new Token[F](
      RequestSession(applicationName, uri, sttpBackend, wrapSttpBackend, authProvider, clientTag)
    )
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
  def rawRows(database: String, table: String, filterNullFields: Boolean = false): RawRows[F] =
    new RawRows(requestSession.withResourceType(RAW_ROWS), database, table, filterNullFields)

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
  lazy val containers =
    new Containers[F](requestSession.withResourceType(DATAMODELS))
  lazy val instances =
    new Instances[F](requestSession.withResourceType(DATAMODELS))
  lazy val views = new Views[F](requestSession.withResourceType(DATAMODELS))
  lazy val spacesv3 = new SpacesV3[F](requestSession.withResourceType(DATAMODELS))
  lazy val dataModelsV3 =
    new DataModelsV3[F](requestSession.withResourceType(DATAMODELS))

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
  case object DATAMODELS extends RESOURCE_TYPE
  case object PROJECT extends RESOURCE_TYPE
  case object GROUPS extends RESOURCE_TYPE
  case object SECURITY_CATEGORIES extends RESOURCE_TYPE

  implicit val projectAuthenticationDecoder: Decoder[ProjectAuthentication] =
    deriveDecoder[ProjectAuthentication]
  @SuppressWarnings(Array("org.wartremover.warts.JavaSerializable"))
  implicit val projectDecoder: Decoder[Project] = deriveDecoder[Project]

  val defaultBaseUrl: String = Option(System.getenv("COGNITE_BASE_URL"))
    .getOrElse("https://api.cognitedata.com")

  def apply[F[_]: Trace](
      applicationName: String,
      projectName: String,
      baseUrl: String,
      auth: Auth,
      sttpBackend: SttpBackend[F, Any],
      wrapSttpBackend: SttpBackend[F, Any] => SttpBackend[F, Any] = identity[SttpBackend[F, Any]](_)
  )(implicit F: CMonadError[F, Throwable]): GenericClient[F] =
    new GenericClient(
      applicationName,
      projectName,
      baseUrl,
      auth,
      sttpBackend = sttpBackend,
      wrapSttpBackend = wrapSttpBackend
    )

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

  def forAuth[F[_]: Trace](
      applicationName: String,
      projectName: String,
      auth: Auth,
      baseUrl: String = defaultBaseUrl,
      apiVersion: Option[String] = None,
      clientTag: Option[String] = None,
      cdfVersion: Option[String] = None,
      sttpBackend: SttpBackend[F, Any]
  )(implicit F: CMonadError[F, Throwable]): F[GenericClient[F]] =
    forAuthProvider(
      applicationName,
      projectName,
      AuthProvider(auth),
      baseUrl,
      apiVersion,
      clientTag,
      cdfVersion,
      sttpBackend
    )

  def forAuthProvider[F[_]: Trace](
      applicationName: String,
      projectName: String,
      authProvider: AuthProvider[F],
      baseUrl: String = defaultBaseUrl,
      apiVersion: Option[String] = None,
      clientTag: Option[String] = None,
      cdfVersion: Option[String] = None,
      sttpBackend: SttpBackend[F, Any],
      wrapSttpBackend: SttpBackend[F, Any] => SttpBackend[F, Any] = identity[SttpBackend[F, Any]](_)
  )(implicit F: CMonadError[F, Throwable]): F[GenericClient[F]] =
    if (projectName.isEmpty) {
      F.raiseError(InvalidAuthentication())
    } else {
      F.pure(
        new GenericClient[F](
          applicationName,
          projectName,
          baseUrl,
          authProvider,
          apiVersion,
          clientTag,
          cdfVersion,
          sttpBackend,
          wrapSttpBackend
        )
      )
    }

  def parseResponse[T, R](uri: Uri, mapResult: T => R)(
      implicit decoder: Decoder[T]
  ): ResponseAs[Either[Throwable, R], Any] =
    asJsonEither[CdpApiError, T]
      .mapWithMetadata(
        (response, metadata) =>
      response
        .leftMap[Throwable] {
          case DeserializationException(_, _)
              if metadata.code.code === StatusCode.TooManyRequests.code =>
            CdpApiException(
              url = uri"$uri",
              code = StatusCode.TooManyRequests.code,
              missing = None,
              duplicated = None,
              missingFields = None,
              message = "Too many requests.",
              requestId = metadata.header("x-request-id"),
              debugNotices = None
            )
          case DeserializationException(_, error) =>
            SdkException(
              s"Failed to parse response, reason: ${error.getMessage}",
              Some(uri),
              metadata.header("x-request-id"),
              Some(metadata.code.code)
            )
          case HttpError(cdpApiError, _) =>
            cdpApiError.asException(uri"$uri", metadata.header("x-request-id"))
        }
        .map(mapResult)
    )
}
