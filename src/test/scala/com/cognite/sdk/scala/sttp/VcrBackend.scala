// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.sttp

import io.circe._
import io.circe.generic.semiauto._
import io.circe.parser._
import io.circe.syntax._
import sttp.capabilities.Effect
import sttp.client3._
import sttp.client3.testing.SttpBackendStub
import sttp.model.{Header, StatusCode}
import sttp.monad.MonadError

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import java.net.URI
import java.nio.file.{Files, Paths}
import java.util.{Base64, Locale, Objects}
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.zip.{GZIPInputStream, GZIPOutputStream}
import scala.jdk.CollectionConverters._

@SuppressWarnings(Array("org.wartremover.warts.PlatformDefault"))
sealed trait VcrMode

@SuppressWarnings(Array("org.wartremover.warts.PlatformDefault"))
object VcrMode {
  case object Record extends VcrMode
  case object Playback extends VcrMode
  case object Auto extends VcrMode
  case object Bypass extends VcrMode

  def fromEnv(default: VcrMode = Playback): VcrMode =
    sys.env.get("VCR_MODE").map(_.toUpperCase(Locale.ROOT)) match {
      case Some("RECORD")   => Record
      case Some("PLAYBACK") => Playback
      case Some("AUTO")     => Auto
      case Some("BYPASS")   => Bypass
      case None             => default
      case Some(other)      => throw new IllegalArgumentException(s"Invalid VCR_MODE: $other")
    }
}

@SuppressWarnings(Array("org.wartremover.warts.PlatformDefault"))
sealed trait RecordedContent {
  def toBytes: Array[Byte]
}

@SuppressWarnings(Array("org.wartremover.warts.PlatformDefault", "org.wartremover.warts.NonUnitStatements"))
object RecordedContent {
  final case class TextContent(body: String) extends RecordedContent {
    def toBytes: Array[Byte] = body.getBytes("UTF-8")
  }

  final case class JsonContent(body: Json) extends RecordedContent {
    def toBytes: Array[Byte] = body.noSpaces.getBytes("UTF-8")
  }

  final case class GzippedContent(gzipped: String) extends RecordedContent {
    def toBytes: Array[Byte] = {
      val compressed = Base64.getDecoder.decode(gzipped)
      val bis = new ByteArrayInputStream(compressed)
      val gz = new GZIPInputStream(bis)
      try gz.readAllBytes()
      finally gz.close()
    }
  }

  private val GzipThreshold = 1000000

  def fromBytes(bytes: Array[Byte], contentType: String): RecordedContent = {
    val mimeType = contentType.split(";")(0).strip().toLowerCase(Locale.ROOT)
    if (bytes.length >= GzipThreshold) {
      val baos = new ByteArrayOutputStream()
      val gz = new GZIPOutputStream(baos)
      gz.write(bytes)
      gz.close()
      GzippedContent(Base64.getEncoder.encodeToString(baos.toByteArray))
    } else {
      mimeType match {
        case "application/json" =>
          parse(new String(bytes, "UTF-8")) match {
            case Right(json) => JsonContent(json)
            case Left(_)     => TextContent(new String(bytes, "UTF-8"))
          }
        case _ => TextContent(new String(bytes, "UTF-8"))
      }
    }
  }

  implicit val encoder: Encoder[RecordedContent] = Encoder.instance {
    case TextContent(body)  => Json.obj("type" -> "text".asJson, "body" -> body.asJson)
    case JsonContent(body)  => Json.obj("type" -> "json".asJson, "body" -> body)
    case GzippedContent(gz) =>
      Json.obj(
        "type"    -> "__gzipped_by_vcr_library_due_to_size".asJson,
        "gzipped" -> gz.asJson
      )
  }

  implicit val decoder: Decoder[RecordedContent] = Decoder.instance { c =>
    c.downField("type").as[String].flatMap {
      case "text" => c.downField("body").as[String].map(s => TextContent(s))
      case "json" => c.downField("body").as[Json].map(j => JsonContent(j))
      case "__gzipped_by_vcr_library_due_to_size" =>
        c.downField("gzipped").as[String].map(s => GzippedContent(s))
      case other => Left(DecodingFailure(s"Unknown content type: $other", c.history))
    }
  }
}

// Codec that matches the Kotlin reference: single-value headers as a bare string, multi-value as array.
object HeaderCodecs {
  private val decodeStringList: Decoder[List[String]] =
    Decoder.decodeList(Decoder.decodeString)

  implicit val headerValuesEncoder: Encoder[List[String]] = Encoder.instance {
    case List(single) => Json.fromString(single)
    case multiple     => Json.arr(multiple.map(Json.fromString): _*)
  }

  implicit val headerValuesDecoder: Decoder[List[String]] =
    Decoder.instance(c => c.as[String].map(List(_)).orElse(decodeStringList.tryDecode(c)))
}

final case class RecordedEntity(
    contentLength: Long,
    contentType: String,
    contentEncoding: Option[String],
    content: RecordedContent
)

final case class RecordedStatus(code: Int, reason: String)

final case class RecordedRequest(
    method: String,
    uri: String,
    headers: Map[String, List[String]],
    entity: Option[RecordedEntity]
)

final case class RecordedResponse(
    method: String,
    uri: String,
    status: RecordedStatus,
    headers: Map[String, List[String]],
    entity: Option[RecordedEntity]
)

final case class Interaction(request: RecordedRequest, response: RecordedResponse)
final case class Cassette(
    interactions: List[Interaction],
    recordedEnv: Map[String, String] = Map.empty
)

object CassetteCodecs {
  import HeaderCodecs._
  import RecordedContent._

  implicit val entityEncoder: Encoder[RecordedEntity] = deriveEncoder
  implicit val entityDecoder: Decoder[RecordedEntity] = deriveDecoder
  implicit val statusEncoder: Encoder[RecordedStatus] = deriveEncoder
  implicit val statusDecoder: Decoder[RecordedStatus] = deriveDecoder
  implicit val recordedRequestEncoder: Encoder[RecordedRequest] = deriveEncoder
  implicit val recordedRequestDecoder: Decoder[RecordedRequest] = deriveDecoder
  implicit val recordedResponseEncoder: Encoder[RecordedResponse] = deriveEncoder
  implicit val recordedResponseDecoder: Decoder[RecordedResponse] = deriveDecoder
  implicit val interactionEncoder: Encoder[Interaction] = deriveEncoder
  implicit val interactionDecoder: Decoder[Interaction] = deriveDecoder
  implicit val cassetteEncoder: Encoder[Cassette] = deriveEncoder
  implicit val cassetteDecoder: Decoder[Cassette] = deriveDecoder
}

final class NoMoreInteractionsException
    extends RuntimeException("No more recorded interactions in cassette")

final class ExtraInteractionsException
    extends RuntimeException(
      "Extra interactions in cassette — not all recorded interactions were consumed"
    )

final class RequestMismatch(details: List[String])
    extends RuntimeException(s"Request mismatch:\n${details.mkString("\n")}")

/** STTP backend that records HTTP interactions to a cassette file (record mode) or replays them
  * from a cassette file without making real HTTP requests (playback mode).
  *
  * The cassette format is compatible with the Kotlin VCR library used elsewhere in this org.
  *
  * Modes are controlled by the VCR_MODE environment variable: RECORD, PLAYBACK, AUTO (default when
  * cassette exists → playback, otherwise → record), or BYPASS (passthrough, no cassette).
  */
@SuppressWarnings(
  Array(
    "org.wartremover.warts.Equals",
    "org.wartremover.warts.NonUnitStatements",
    "org.wartremover.warts.PlatformDefault"
  )
)
class VcrBackend[F[_]](
    delegate: SttpBackend[F, Any],
    cassettePath: String,
    mode: VcrMode
) extends SttpBackend[F, Any] {

  import CassetteCodecs._

  private val recordedInteractions = new ConcurrentLinkedQueue[Interaction]()

  val actualMode: VcrMode = mode match {
    case VcrMode.Auto =>
      sys.env.get("VCR_MODE").map(_.toUpperCase(Locale.ROOT)) match {
        case Some("RECORD")   => VcrMode.Record
        case Some("BYPASS")   => VcrMode.Bypass
        case Some("PLAYBACK") => VcrMode.Playback
        case _ =>
          if (Files.exists(Paths.get(cassettePath))) VcrMode.Playback else VcrMode.Record
      }
    case other => other
  }

  private lazy val cassetteInteractions: Iterator[Interaction] = loadCassette().iterator

  private def loadCassette(): List[Interaction] = {
    val path = Paths.get(cassettePath)
    if (!Files.exists(path))
      throw new IllegalStateException(
        s"Cassette file does not exist: $cassettePath. Run in RECORD mode first."
      )
    val content = new String(Files.readAllBytes(path), "UTF-8")
    decode[Cassette](content) match {
      case Right(cassette) => cassette.interactions
      case Left(e) =>
        throw new RuntimeException(
          s"Failed to decode cassette at $cassettePath. " +
            s"If using git LFS, ensure the file is checked out. Error: $e"
        )
    }
  }

  override def send[T, R >: Any with Effect[F]](request: Request[T, R]): F[Response[T]] =
    actualMode match {
      case VcrMode.Bypass   => delegate.send(request)
      case VcrMode.Record   => record(request)
      case VcrMode.Playback => playback(request)
      case VcrMode.Auto     =>
        responseMonad.error[Response[T]](
          new IllegalStateException("Auto mode should have been resolved")
        )
    }

  private def record[T, R >: Any with Effect[F]](request: Request[T, R]): F[Response[T]] = {
    val bytesRequest = request.response(asByteArrayAlways)
    responseMonad.flatMap(delegate.send(bytesRequest)) { rawResponse =>
      val bodyBytes = rawResponse.body
      val _ = recordedInteractions.add(buildInteraction(request, rawResponse, bodyBytes))
      val stub = SttpBackendStub[F, Any](responseMonad)
        .whenAnyRequest
        .thenRespond(bodyBytes, rawResponse.code)
      responseMonad.map(stub.send(request))(_.copy(headers = rawResponse.headers))
    }
  }

  private def playback[T, R >: Any with Effect[F]](request: Request[T, R]): F[Response[T]] =
    if (!cassetteInteractions.hasNext)
      responseMonad.error[Response[T]](new NoMoreInteractionsException())
    else {
      val interaction = cassetteInteractions.next()
      validateRequest(request, interaction.request) match {
        case Some(err) => responseMonad.error[Response[T]](err)
        case None =>
          val bytes =
            interaction.response.entity.map(_.content.toBytes).getOrElse(Array.empty[Byte])
          val status = StatusCode(interaction.response.status.code)
          val respHeaders = toSttpHeaders(interaction.response.headers)
          val stub = SttpBackendStub[F, Any](responseMonad)
            .whenAnyRequest
            .thenRespond(bytes, status)
          responseMonad.map(stub.send(request))(_.copy(headers = respHeaders))
      }
    }

  private def validateRequest(
      request: Request[_, _],
      recorded: RecordedRequest
  ): Option[RequestMismatch] = {
    val methodError = Option.when(!request.method.method.equals(recorded.method))(
      s"Method mismatch: expected ${recorded.method}, got ${request.method.method}"
    )

    val uriError = Option.when(!urisMatchIgnoringPort(request.uri.toString, recorded.uri))(
      s"URI mismatch:\n  expected: ${recorded.uri}\n  actual:   ${request.uri}"
    )

    val actualBody = extractRequestBody(request)
    val expectedBody = recorded.entity.map(_.content.toBytes)

    val bodyError = (actualBody, expectedBody) match {
      case (Some(actual), Some(expected)) =>
        Option.when(!bodiesMatch(actual, expected))(bodyMismatchMessage(actual, expected))
      case (None, Some(_)) => Some("Expected a request body but got none")
      case (Some(_), None) => Some("Got a request body but expected none")
      case _               => None
    }

    val errors = List(methodError, uriError, bodyError).flatten
    if (errors.isEmpty) None else Some(new RequestMismatch(errors))
  }

  private def urisMatchIgnoringPort(uri1: String, uri2: String): Boolean =
    try {
      val u1 = new URI(uri1)
      val u2 = new URI(uri2)
      Objects.equals(u1.getScheme, u2.getScheme) &&
        Objects.equals(u1.getHost, u2.getHost) &&
        Objects.equals(u1.getPath, u2.getPath) &&
        Objects.equals(u1.getQuery, u2.getQuery) &&
        Objects.equals(u1.getFragment, u2.getFragment) &&
        Objects.equals(u1.getUserInfo, u2.getUserInfo)
    } catch { case _: Exception => uri1.equals(uri2) }

  private def extractRequestBody(request: Request[_, _]): Option[Array[Byte]] =
    request.body match {
      case StringBody(s, enc, _) =>
        val bytes = s.getBytes(enc)
        if (bytes.isEmpty) None else Some(bytes)
      case ByteArrayBody(ba, _) => if (ba.isEmpty) None else Some(ba)
      case NoBody               => None
      case _                    => None
    }

  private def bodiesMatch(actual: Array[Byte], expected: Array[Byte]): Boolean = {
    val actualStr = new String(actual, "UTF-8")
    val expectedStr = new String(expected, "UTF-8")
    if (actualStr.equals(expectedStr)) true
    else
      (parse(actualStr), parse(expectedStr)) match {
        case (Right(j1), Right(j2)) => j1.equals(j2)
        case _                      => false
      }
  }

  private def bodyMismatchMessage(actual: Array[Byte], expected: Array[Byte]): String = {
    val actualStr = new String(actual, "UTF-8")
    val expectedStr = new String(expected, "UTF-8")
    (parse(actualStr), parse(expectedStr)) match {
      case (Right(aj), Right(ej)) =>
        jsonDiff(aj, ej, "$")
          .map(d => s"Body mismatch at $d")
          .getOrElse(s"Body mismatch (JSON is semantically equal but raw bytes differ)")
      case _ =>
        s"Body mismatch:\n  expected: $expectedStr\n  actual:   $actualStr"
    }
  }

  @SuppressWarnings(Array("org.wartremover.warts.Recursion"))
  private def jsonDiff(actual: Json, expected: Json, path: String): Option[String] = {
    if (actual == expected) None
    else
      (actual.asObject, expected.asObject) match {
        case (Some(ao), Some(eo)) =>
          val missing = eo.keys.toVector.filterNot(k => ao.contains(k))
          val extra   = ao.keys.toVector.filterNot(k => eo.contains(k))
          missing.headOption
            .map(k => s"$path.$k: expected ${eo(k).fold("null")(_.noSpaces)}, got missing")
            .orElse(
              extra.headOption.map(k =>
                s"$path.$k: expected missing, got ${ao(k).fold("null")(_.noSpaces)}"
              )
            )
            .orElse(
              eo.toIterable
                .flatMap { case (k, ev) => ao(k).flatMap(av => jsonDiff(av, ev, s"$path.$k")) }
                .headOption
            )
        case _ =>
          (actual.asArray, expected.asArray) match {
            case (Some(aa), Some(ea)) =>
              if (aa.lengthIs != ea.length)
                Some(s"$path: expected array of length ${ea.length}, got ${aa.length}")
              else
                aa.zip(ea).zipWithIndex
                  .flatMap { case ((av, ev), i) => jsonDiff(av, ev, s"$path[$i]") }
                  .headOption
            case _ =>
              Some(s"$path: expected ${expected.noSpaces}, got ${actual.noSpaces}")
          }
      }
  }

  private def buildInteraction(
      request: Request[_, _],
      rawResponse: Response[Array[Byte]],
      bodyBytes: Array[Byte]
  ): Interaction = {
    val reqEntity = extractRequestBody(request).map { bytes =>
      val ct =
        request.headers
          .find(_.name.equalsIgnoreCase("content-type"))
          .map(_.value)
          .getOrElse("application/octet-stream")
      RecordedEntity(bytes.length.toLong, ct, None, RecordedContent.fromBytes(bytes, ct))
    }

    val respEntity =
      if (bodyBytes.nonEmpty) {
        val ct =
          rawResponse.headers
            .find(_.name.equalsIgnoreCase("content-type"))
            .map(_.value)
            .getOrElse("application/octet-stream")
        Some(
          RecordedEntity(
            bodyBytes.length.toLong,
            ct,
            None,
            RecordedContent.fromBytes(bodyBytes, ct)
          )
        )
      } else None

    Interaction(
      RecordedRequest(
        method = request.method.method,
        uri = request.uri.toString,
        headers = toHeaderMap(request.headers),
        entity = reqEntity
      ),
      RecordedResponse(
        method = request.method.method,
        uri = request.uri.toString,
        status = RecordedStatus(rawResponse.code.code, rawResponse.statusText),
        headers = toHeaderMap(rawResponse.headers),
        entity = respEntity
      )
    )
  }

  private val SensitiveHeaders: Set[String] =
    Set("authorization", "x-api-key", "cookie", "set-cookie")

  private def toHeaderMap(headers: Seq[Header]): Map[String, List[String]] =
    headers
      .filterNot(h => SensitiveHeaders.contains(h.name.toLowerCase(Locale.ROOT)))
      .groupBy(_.name.toLowerCase(Locale.ROOT))
      .view
      .mapValues(hs => hs.map(_.value).toList)
      .toMap

  private def toSttpHeaders(headers: Map[String, List[String]]): Seq[Header] =
    headers.flatMap { case (k, vs) => vs.map(v => Header(k, v)) }.toSeq

  private def flushCassette(): Unit = {
    val cassette = Cassette(recordedInteractions.asScala.toList)
    val json = cassette.asJson.spaces2
    val path = Paths.get(cassettePath)
    Option(path.getParent).foreach(p => Files.createDirectories(p))
    val _ = Files.write(path, json.getBytes("UTF-8"))
  }

  override def close(): F[Unit] = {
    actualMode match {
      case VcrMode.Record => flushCassette()
      case _              => ()
    }
    delegate.close()
  }

  override def responseMonad: MonadError[F] = delegate.responseMonad
}
