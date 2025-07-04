// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.common

import java.time.Instant
import cats.Id
import com.cognite.sdk.scala.v1.{CogniteId, CogniteInstanceId}
import io.circe.{Decoder, Encoder, Json, JsonObject, KeyEncoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import sttp.model.Uri

trait ResponseWithCursor {
  val nextCursor: Option[String]
}
final case class ItemsWithCursor[A](items: Seq[A], nextCursor: Option[String] = None)
    extends ResponseWithCursor
object ItemsWithCursor {
  implicit def itemsWithCursorEncoder[A: Encoder]: Encoder[ItemsWithCursor[A]] =
    deriveEncoder[ItemsWithCursor[A]]
  implicit def itemsWithCursorDecoder[A: Decoder]: Decoder[ItemsWithCursor[A]] =
    deriveDecoder[ItemsWithCursor[A]]
}
final case class Items[A](items: Seq[A])
object Items {
  implicit def itemsEncoder[A: Encoder]: Encoder[Items[A]] = deriveEncoder[Items[A]]
  implicit def itemsDecoder[A: Decoder]: Decoder[Items[A]] = deriveDecoder[Items[A]]
}
final case class ItemsWithIgnoreUnknownIds[A](items: Seq[A], ignoreUnknownIds: Boolean)
final case class ItemsWithRecursiveAndIgnoreUnknownIds(
    items: Seq[CogniteId],
    recursive: Boolean,
    ignoreUnknownIds: Boolean
)

final case class SdkException(
    message: String,
    uri: Option[Uri] = None,
    requestId: Option[String] = None,
    responseCode: Option[Int] = None
) extends Throwable(SdkException.formatMessage(message, uri, requestId, responseCode))
object SdkException {
  def formatMessage(
      message: String,
      uri: Option[Uri],
      requestId: Option[String],
      responseCode: Option[Int]
  ): String = {
    val responseCodeMessage = responseCode
      .map(c => s" (with response code ${c.toString})")
      .getOrElse("")
    val uriMessage = uri
      .map(u => s", in response$responseCodeMessage to request sent to ${u.toString}")
      .getOrElse(responseCodeMessage)
    val requestIdMessage = requestId.map(id => s", with request id $id").getOrElse("")
    val exceptionMessage = if (message.nonEmpty) {
      message
    } else {
      "Missing error message"
    }
    s"$exceptionMessage$uriMessage$requestIdMessage"
  }
}

final case class CdpApiErrorPayload(
    code: Int,
    message: String,
    missing: Option[Seq[JsonObject]],
    duplicated: Option[Seq[JsonObject]],
    missingFields: Option[Seq[String]],
    extra: Option[Extra]
)

final case class CdpApiError(error: CdpApiErrorPayload) {
  def asException(url: Uri, requestId: Option[String]): CdpApiException =
    CdpApiException(
      url,
      error.code,
      error.message,
      error.missing,
      error.duplicated,
      error.missingFields,
      requestId,
      error.extra
    )
}

object CdpApiError {
  implicit val errorExtraDecoder: Decoder[Extra] = deriveDecoder

  implicit val cdpApiErrorPayloadDecoder: Decoder[CdpApiErrorPayload] = deriveDecoder
  implicit val cdpApiErrorDecoder: Decoder[CdpApiError] = deriveDecoder
}

final case class Extra(
    hint: Option[String] = None
)
final case class CdpApiException(
    url: Uri,
    code: Int,
    message: String,
    missing: Option[Seq[JsonObject]],
    duplicated: Option[Seq[JsonObject]],
    missingFields: Option[Seq[String]],
    requestId: Option[String],
    extra: Option[Extra] = None
) extends Throwable({
      import CdpApiException._
      val maybeId = requestId.map(id => s"with id $id ").getOrElse("")
      val maybeHint = extra.flatMap(e => e.hint.map(h => s" Hint: $h")).getOrElse("")

      val details = Seq(
        missingFields.map(fields => s" Missing fields: [${fields.mkString(", ")}]."),
        duplicated.map(describeErrorList("Duplicated")),
        missing.map(describeErrorList("Missing"))
      ).flatMap(_.toList).mkString

      s"Request ${maybeId}to ${url.toString} failed with status ${code.toString}: $message.$details$maybeHint"
    })

object CdpApiException {
  private def describeErrorList(kind: String)(items: Seq[JsonObject]): String =
    items
      .flatMap(_.toIterable)
      .groupBy { case (key, _) => key }
      .toList
      .sortBy { case (key, _) => key } // Ensure deterministic ordering
      .map { case (key, entries) =>
        // Example:
        //    Duplicated ids: [1234567, 1234568]. Duplicated externalIds: [externalId-1, externalId-2].

        val commaSeparatedValues =
          entries
            .map { case (_, value) =>
              value.asString.getOrElse(value.toString)
            } // Print strings without quotes
            .sorted // Ensure deterministic ordering
            .mkString(", ")

        s" $kind ${key}s: [$commaSeparatedValues]."
      }
      .mkString
}

final case class DataPoint(
    timestamp: Instant,
    value: Double
)

final case class AggregateDataPoint(
    timestamp: Instant,
    average: Option[Double],
    max: Option[Double],
    min: Option[Double],
    count: Option[Double],
    sum: Option[Double],
    interpolation: Option[Double],
    stepInterpolation: Option[Double],
    totalVariation: Option[Double],
    continuousVariance: Option[Double],
    discreteVariance: Option[Double]
)

final case class StringDataPoint(
    timestamp: Instant,
    value: String
)

trait WithId[I] {
  val id: I
}

trait WithGetExternalId {
  def getExternalId: Option[String]
}

trait WithGetInstanceId {
  def getInstanceId: Option[CogniteInstanceId]
}

trait WithExternalIdGeneric[F[_]] extends WithGetExternalId {
  val externalId: F[String]
}

trait WithInstanceIdGeneric[F[_]] extends WithGetInstanceId {
  val instanceId: F[CogniteInstanceId]
}

trait WithExternalId extends WithExternalIdGeneric[Option] {
  def getExternalId: Option[String] = externalId
}

trait WithRequiredExternalId extends WithExternalIdGeneric[Id] {
  def getExternalId: Option[String] = Some(externalId)
}

trait WithCreatedTime {
  val createdTime: Instant
}

trait WithSetExternalId extends WithGetExternalId {
  val externalId: Option[Setter[String]]
  override def getExternalId: Option[String] =
    externalId match {
      case Some(SetValue(v)) => Some(v)
      case _ => None
    }
}

trait ToCreate[W] {
  def toCreate: W
}

trait ToUpdate[U] {
  def toUpdate: U
}

object EitherDecoder {
  def eitherDecoder[A, B](implicit a: Decoder[A], b: Decoder[B]): Decoder[Either[A, B]] = {
    val l: Decoder[Either[A, B]] = a.map(Left.apply)
    val r: Decoder[Either[A, B]] = b.map(Right.apply)
    l.or(r)
  }
}

sealed trait Setter[+T]
sealed trait NonNullableSetter[+T]
final case class SetValue[+T](set: T) extends Setter[T] with NonNullableSetter[T]
final case class SetNull[+T]() extends Setter[T]

// For add/remove on updates for assets_ids, labels (array types)
final case class UpdateArray[+T](add: Seq[T] = Seq.empty, remove: Seq[T] = Seq.empty)
    extends NonNullableSetter[Seq[T]]
// For metadata add/remove on updates
final case class UpdateMap(add: Map[String, String] = Map.empty, remove: Seq[String] = Seq.empty)
    extends NonNullableSetter[Map[String, String]]

object Setter {
  @SuppressWarnings(Array("org.wartremover.warts.Null"))
  def fromOption[T](option: Option[T]): Option[Setter[T]] =
    option match {
      case null => Some(SetNull())
      case None => None
      case Some(null) => Some(SetNull())
      case Some(value) => Some(SetValue(value))
    }

  @SuppressWarnings(Array("org.wartremover.warts.Null"))
  def fromAny[T](optionValue: T): Option[Setter[T]] =
    optionValue match {
      case null => Some(SetNull())
      case value => Some(SetValue(value))
    }

  implicit def encodeSetter[T](implicit encodeT: Encoder[T]): Encoder[Setter[T]] = {
    case SetValue(value) => Json.obj(("set", encodeT.apply(value)))
    case SetNull() => Json.obj(("setNull", Json.True))
  }
}

object NonNullableSetter {
  @SuppressWarnings(
    Array(
      "org.wartremover.warts.Null",
      "org.wartremover.warts.Equals",
      "org.wartremover.warts.OptionPartial",
      "scalafix:DisableSyntax.!="
    )
  )
  def fromOption[T](option: Option[T]): Option[NonNullableSetter[T]] =
    option match {
      case None => None
      case Some(value) =>
        require(
          value != null,
          "Invalid null value for non-nullable field update"
        )
        Some(SetValue(value))
    }

  def fromAny[T](value: T): NonNullableSetter[T] = SetValue(value)

  implicit def encodeNonNullableSetterSeq[T](
      implicit encodeT: Encoder[T]
  ): Encoder[NonNullableSetter[Seq[T]]] = new Encoder[NonNullableSetter[Seq[T]]] {
    private val encodeSeqT = Encoder.encodeSeq(encodeT)

    final def apply(a: NonNullableSetter[Seq[T]]): Json = a match {
      case SetValue(value) =>
        Json.obj("set" -> encodeSeqT(value))
      case UpdateArray(add: Seq[T] @unchecked, remove: Seq[T] @unchecked) =>
        Json.obj("add" -> encodeSeqT(add), "remove" -> encodeSeqT(remove))
    }
  }

  private val encodeMapStringString =
    Encoder.encodeMap[String, String](KeyEncoder.encodeKeyString, Encoder.encodeString)
  private val encodeSeqString = Encoder.encodeSeq[String]
  implicit def encodeNonNullableSetterMapStringString
      : Encoder[NonNullableSetter[Map[String, String]]] = {
    case SetValue(value) =>
      Json.obj("set" -> encodeMapStringString(value))
    case UpdateMap(add, remove) =>
      Json.obj("add" -> encodeMapStringString(add), "remove" -> encodeSeqString(remove))
  }

  implicit def encodeNonNullableSetter[T](
      implicit encodeT: Encoder[T]
  ): Encoder[NonNullableSetter[T]] = {
    case SetValue(value) =>
      Json.obj("set" -> encodeT(value))
    case _ =>
      throw new RuntimeException("Invalid NonNullableSetter. This should never happen.")
  }

}
