// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.common

import java.time.Instant
import cats.Id
import com.cognite.sdk.scala.v1.CogniteId
import io.circe.{Decoder, Encoder, Json, JsonObject}
import io.circe.generic.semiauto.deriveDecoder
import sttp.model.Uri
// scalastyle:off number.of.types
trait ResponseWithCursor {
  val nextCursor: Option[String]
}
final case class ItemsWithCursor[A](items: Seq[A], nextCursor: Option[String] = None)
    extends ResponseWithCursor
final case class Items[A](items: Seq[A])
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
      .map(u => s", in response$responseCodeMessage to request sent to ${u.toString()}")
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
    missingFields: Option[Seq[String]]
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
      requestId
    )
}

object CdpApiError {
  implicit val cdpApiErrorPayloadDecoder: Decoder[CdpApiErrorPayload] = deriveDecoder
  implicit val cdpApiErrorDecoder: Decoder[CdpApiError] = deriveDecoder
}

final case class CdpApiException(
    url: Uri,
    code: Int,
    message: String,
    missing: Option[Seq[JsonObject]],
    duplicated: Option[Seq[JsonObject]],
    missingFields: Option[Seq[String]],
    requestId: Option[String]
) extends Throwable({
      import CdpApiException._
      val maybeId = requestId.map(id => s"with id $id ").getOrElse("")
      val details = Seq(
        missingFields.map(fields => s" Missing fields: [${fields.mkString(", ")}]."),
        duplicated.map(describeErrorList("Duplicated")),
        missing.map(describeErrorList("Missing"))
      ).flatMap(_.toList).mkString

      s"Request ${maybeId}to ${url.toString} failed with status ${code.toString}: $message.$details"
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
  def getExternalId(): Option[String]
}

trait WithExternalIdGeneric[F[_]] extends WithGetExternalId {
  val externalId: F[String]
}

trait WithExternalId extends WithExternalIdGeneric[Option] {
  def getExternalId(): Option[String] = externalId
}

trait WithRequiredExternalId extends WithExternalIdGeneric[Id] {
  def getExternalId(): Option[String] = Some(externalId)
}

trait WithCreatedTime {
  val createdTime: Instant
}

trait WithSetExternalId extends WithGetExternalId {
  val externalId: Option[Setter[String]]
  override def getExternalId(): Option[String] =
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
  @SuppressWarnings(Array("org.wartremover.warts.Null", "scalafix:DisableSyntax.null"))
  def fromOption[T](option: Option[T]): Option[Setter[T]] =
    option match {
      case null => Some(SetNull()) // scalastyle:ignore null
      case None => None
      case Some(null) => Some(SetNull()) // scalastyle:ignore null
      case Some(map: Map[_, _]) if map.isEmpty =>
        // Workaround for CDF-3540 and CDF-953
        None
      case Some(value) => Some(SetValue(value))
    }

  @SuppressWarnings(Array("org.wartremover.warts.Null", "scalafix:DisableSyntax.null"))
  def fromAny[T](optionValue: T): Option[Setter[T]] =
    optionValue match {
      case null => Some(SetNull()) // scalastyle:ignore null
      case value => Some(SetValue(value))
    }

  implicit def encodeSetter[T](implicit encodeT: Encoder[T]): Encoder[Setter[T]] =
    new Encoder[Setter[T]] {
      final def apply(a: Setter[T]): Json = a match {
        case SetValue(value) => Json.obj(("set", encodeT.apply(value)))
        case SetNull() => Json.obj(("setNull", Json.True))
      }
    }
}

object NonNullableSetter {
  @SuppressWarnings(
    Array(
      "org.wartremover.warts.Null",
      "org.wartremover.warts.Equals",
      "org.wartremover.warts.OptionPartial",
      "scalafix:DisableSyntax.null",
      "scalafix:DisableSyntax.!="
    )
  )
  def fromOption[T](option: Option[T]): Option[NonNullableSetter[T]] =
    option match {
      case None => None
      case Some(map: Map[_, _]) if map.isEmpty =>
        // Workaround for CDF-3540 and CDF-953
        None
      case Some(value) =>
        require(
          value != null, // scalastyle:ignore null
          "Invalid null value for non-nullable field update"
        )
        Some(SetValue(value))
    }

  def fromAny[T](value: T): NonNullableSetter[T] = SetValue(value)

  @SuppressWarnings(
    Array(
      "org.wartremover.warts.Null",
      "scalafix:DisableSyntax.null"
    )
  )
  implicit def encodeNonNullableSetter[T](
      implicit encodeT: Encoder[T]
  ): Encoder[NonNullableSetter[T]] = new Encoder[NonNullableSetter[T]] {
    final def apply(a: NonNullableSetter[T]): Json = a match {
      case SetValue(value) => Json.obj(("set", encodeT.apply(value)))
      case UpdateArray(_, _) => null // scalastyle:ignore null
      case UpdateMap(_, _) => null // scalastyle:ignore null
    }
  }

  implicit def encodeUpdateMap(
      implicit encodeT: Encoder[Map[String, String]]
  ): Encoder[NonNullableSetter[Map[String, String]]] =
    new Encoder[NonNullableSetter[Map[String, String]]] {
      final def apply(a: NonNullableSetter[Map[String, String]]): Json = a match {
        case SetValue(value) =>
          Json.obj(("set", encodeT.apply(value)))
        case UpdateMap(add, remove) =>
          Json.obj(
            "add" -> encodeT.apply(add),
            "remove" -> Json.arr(remove.map(Json.fromString): _*)
          )
      }
    }

  implicit def encodeUpdateArray[T](
      implicit encodeT: Encoder[T]
  ): Encoder[NonNullableSetter[Seq[T]]] = new Encoder[NonNullableSetter[Seq[T]]] {
    final def apply(a: NonNullableSetter[Seq[T]]): Json = a match {
      case SetValue(value) =>
        Json.obj(("set", Json.arr(value.map(encodeT.apply): _*)))
      case UpdateArray(add, remove) =>
        Json.obj(
          "add" -> Json.arr(add.map(encodeT.apply): _*),
          "remove" -> Json.arr(remove.map(encodeT.apply): _*)
        )
    }
  }
}
