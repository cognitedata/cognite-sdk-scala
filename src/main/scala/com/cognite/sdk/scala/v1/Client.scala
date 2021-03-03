// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1

import BuildInfo.BuildInfo
import cats.{Id, Monad}
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

class AuthSttpBackend[R[_], -S](delegate: SttpBackend[R, S], authProvider: AuthProvider[R])
    extends SttpBackend[R, S] {
  override def send[T](request: Request[T, S]): R[Response[T]] =
    responseMonad.flatMap(authProvider.getAuth) { auth: Auth =>
      delegate.send(auth.auth(request))
    }

  override def close(): Unit = delegate.close()

  override def responseMonad: MonadError[R] = delegate.responseMonad
}

final case class RequestSession[F[_]: Monad](
    applicationName: String,
    baseUrl: Uri,
    baseSttpBackend: SttpBackend[F, _],
    auth: AuthProvider[F],
    clientTag: Option[String] = None
) {
  val sttpBackend: SttpBackend[F, _] = new AuthSttpBackend(baseSttpBackend, auth)

  def send[R](r: RequestT[Empty, String, Nothing] => RequestT[Id, R, Nothing]): F[Response[R]] =
    r(
      sttp
        .readTimeout(90.seconds)
    ).send()(sttpBackend, implicitly)

  private val sttpRequest = {
    val baseRequest = sttp
      .followRedirects(false)
      .header("x-cdp-sdk", s"CogniteScalaSDK:${BuildInfo.version}")
      .header("x-cdp-app", applicationName)
      .readTimeout(90.seconds)
      .parseResponseIfMetadata { md =>
        md.contentLength.forall(_ > 0) && md.contentType.exists(
          // This is a bit ugly, but we're highly unlikely to use any other ContentType
          // any time soon.
          ct => ct.startsWith(MediaTypes.Json) || ct.startsWith("application/protobuf")
        )
      }
    clientTag match {
      case Some(tag) => baseRequest.header("x-cdp-clienttag", tag)
      case None => baseRequest
    }
  }

  private def parseResponse[T, R](uri: Uri, mapResult: T => R)(
      implicit decoder: Decoder[Either[CdpApiError, T]]
  ) =
    asJson[Either[CdpApiError, T]].mapWithMetadata((response, metadata) =>
      response match {
        case Left(value) =>
          // As of 2020-02-17, content length is not reliably set.
          // Checking the content type explicitly for JSON and protobuf isn't
          // ideal either, but we're not likely to add other content types
          // any time soon.
          val shouldHaveParsed = // metadata.contentLength.exists(_ > 0) &&
            metadata.contentType.exists(_.startsWith(MediaTypes.Json)) ||
              metadata.contentType.exists(_.startsWith("application/protobuf"))
          if (shouldHaveParsed) {
            throw SdkException(
              s"Failed to parse response, reason: ${value.error.getMessage}",
              Some(uri),
              metadata.header("x-request-id"),
              Some(metadata.code)
            )
          } else {
            val message = if (metadata.statusText.isEmpty) {
              "Unknown error (no status text)"
            } else {
              metadata.statusText
            }
            throw SdkException(
              message,
              Some(uri),
              metadata.header("x-request-id"),
              Some(metadata.code)
            )
          }
        case Right(Left(cdpApiError)) =>
          throw cdpApiError.asException(uri"$uri", metadata.header("x-request-id"))
        case Right(Right(value)) => mapResult(value)
      }
    )

  private def unsafeBody[R](uri: Uri, response: Response[R]): R =
    try response.unsafeBody
    catch {
      // sttp's .unsafeBody returns a NoSuchElementException if the status wasn't
      // a 200 and there is no body to return.
      case _: NoSuchElementException =>
        val code = response.code.toString
        throw SdkException(
          s"Unexpected status code $code",
          Some(uri),
          response.header("x-request-id"),
          Some(response.code)
        )
      case NonFatal(e) =>
        throw SdkException(
          s"Unexpected exception ${e.toString} while reading response body",
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

class GenericClient[F[_]](
    applicationName: String,
    val projectName: String,
    baseUrl: String,
    authProvider: AuthProvider[F],
    apiVersion: Option[String],
    clientTag: Option[String]
)(implicit monad: Monad[F], sttpBackend: SttpBackend[F, Nothing]) {
  def this(
      applicationName: String,
      projectName: String,
      baseUrl: String = GenericClient.defaultBaseUrl,
      auth: Auth = Auth.defaultAuth,
      apiVersion: Option[String] = None,
      clientTag: Option[String] = None
  )(implicit monad: Monad[F], sttpBackend: SttpBackend[F, Nothing]) =
    this(applicationName, projectName, baseUrl, AuthProvider[F](auth), apiVersion, clientTag)

  import GenericClient._

  val uri: Uri = parseBaseUrlOrThrow(baseUrl)

  lazy val requestSession =
    RequestSession(
      applicationName,
      uri"$uri/api/${apiVersion.getOrElse("v1")}/projects/$projectName",
      sttpBackend,
      authProvider,
      clientTag
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

  def project: F[Project] = {
    implicit val errorOrItemsDecoder: Decoder[Either[CdpApiError, Project]] =
      EitherDecoder.eitherDecoder[CdpApiError, Project]
    requestSession.get[Project, Project](
      requestSession.baseUrl,
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

  val defaultBaseUrl: String = Option(System.getenv("COGNITE_BASE_URL"))
    .getOrElse("https://api.cognitedata.com")

  def apply[F[_]: Monad](applicationName: String, projectName: String, baseUrl: String, auth: Auth)(
      implicit sttpBackend: SttpBackend[F, Nothing]
  ): GenericClient[F] =
    new GenericClient(applicationName, projectName, baseUrl, auth)(implicitly, sttpBackend)

  def parseBaseUrlOrThrow(baseUrl: String): Uri =
    try uri"$baseUrl"
    catch {
      case NonFatal(_) =>
        throw new IllegalArgumentException(
          s"Unable to parse baseUrl = ${baseUrl} as a valid URI."
        )
    }

  def forAuth[F[_]: Monad](
      applicationName: String,
      auth: Auth,
      baseUrl: String = defaultBaseUrl,
      apiVersion: Option[String] = None,
      clientTag: Option[String] = None
  )(implicit sttpBackend: SttpBackend[F, Nothing]): F[GenericClient[F]] =
    forAuthProvider(applicationName, AuthProvider(auth), baseUrl, apiVersion, clientTag)

  def forAuthProvider[F[_]: Monad](
      applicationName: String,
      authProvider: AuthProvider[F],
      baseUrl: String = defaultBaseUrl,
      apiVersion: Option[String] = None,
      clientTag: Option[String] = None
  )(implicit sttpBackend: SttpBackend[F, Nothing]): F[GenericClient[F]] = {
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
          clientTag
        )(
          implicitly,
          sttpBackend
        )
      }
  }
}

class Client(
    applicationName: String,
    override val projectName: String,
    baseUrl: String =
      Option(System.getenv("COGNITE_BASE_URL")).getOrElse("https://api.cognitedata.com"),
    auth: Auth = Auth.defaultAuth
)(implicit sttpBackend: SttpBackend[Id, Nothing])
    extends GenericClient[Id](applicationName, projectName, baseUrl, auth)

object Client {
  def apply(applicationName: String, projectName: String, baseUrl: String, auth: Auth)(
      implicit sttpBackend: SttpBackend[Id, Nothing]
  ): Client = new Client(applicationName, projectName, baseUrl, auth)(sttpBackend)
}
