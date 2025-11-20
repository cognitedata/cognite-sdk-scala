package com.cognite.sdk.scala.v1

import cats.implicits.{toFlatMapOps, toFunctorOps}
import cats.{Id, MonadError => CMonadError}
import com.cognite.scala_sdk.BuildInfo
import com.cognite.sdk.scala.common.AuthProvider
import com.cognite.sdk.scala.v1.GenericClient.parseResponse
import io.circe.Decoder
import natchez.Trace
import sttp.client3.{
  BodySerializer,
  Empty,
  RequestT,
  Response,
  SttpBackend,
  basicRequest,
  emptyRequest
}
import sttp.model.{Header, Uri}

import scala.concurrent.duration.DurationInt

class RequestSessionImplicits[F[_]](
    implicit val FMonad: CMonadError[F, Throwable],
    val FTrace: Trace[F]
)

final case class RequestSession[F[_]: Trace](
    applicationName: String,
    baseUrl: Uri,
    baseSttpBackend: SttpBackend[F, _],
    auth: AuthProvider[F],
    clientTag: Option[String] = None,
    cdfVersion: Option[String] = None,
    tags: Map[String, Any] = Map.empty
)(implicit F: CMonadError[F, Throwable]) {
  val implicits: RequestSessionImplicits[F] = new RequestSessionImplicits[F]
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
      .flatMap(r => F.fromEither(r.body))

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
      .flatMap(r => F.fromEither(r.body))

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
      .flatMap(r => F.fromEither(r.body))

  def head(
      uri: Uri,
      overrideHeaders: Seq[Header] = Seq()
  ): F[Seq[Header]] =
    sttpRequest
      .headers(overrideHeaders: _*)
      .head(uri)
      .send(sttpBackend)
      .map(_.headers)

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
}
