// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.common

import java.time.Instant

import cats.Id
import com.cognite.sdk.scala.v1.CogniteId
import com.softwaremill.sttp.Uri
import io.circe.{Decoder, Encoder, Json, JsonObject}
import io.circe.derivation.deriveDecoder
import io.scalaland.chimney.Transformer
import io.scalaland.chimney.dsl._

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
    val exceptionMessage = if (message.length > 0) {
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
    this.error
      .into[CdpApiException]
      .withFieldConst(_.url, url)
      .withFieldConst(_.requestId, requestId)
      .transform
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

trait WithExternalIdGeneric[F[_]] {
  val externalId: F[String]
  def getExternalId(): Option[String] // We could have implemented Foldable instead
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

trait WithSetExternalId {
  val externalId: Option[Setter[String]]
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

object Setter {
  @SuppressWarnings(Array("org.wartremover.warts.Null", "scalafix:DisableSyntax.null"))
  implicit def optionToSetter[T: Manifest]: Transformer[Option[T], Option[Setter[T]]] =
    new Transformer[Option[T], Option[Setter[T]]] {
      override def transform(src: Option[T]) = src match {
        case null => Some(SetNull()) // scalastyle:ignore null
        case None => None
        case Some(null) => Some(SetNull()) // scalastyle:ignore null
        case Some(map: Map[_, _]) if map.isEmpty =>
          // Workaround for CDF-3540 and CDF-953
          None
        case Some(value: T) => Some(SetValue(value))
        case Some(badValue) =>
          throw new SdkException(
            s"Expected value of type ${manifest[T].toString} but got `${badValue.toString}` of type ${badValue.getClass.toString}"
          )
      }
    }

  @SuppressWarnings(Array("org.wartremover.warts.Null", "scalafix:DisableSyntax.null"))
  implicit def anyToSetter[T]: Transformer[T, Option[Setter[T]]] =
    new Transformer[T, Option[Setter[T]]] {
      override def transform(src: T): Option[Setter[T]] = src match {
        case null => Some(SetNull()) // scalastyle:ignore null
        case value => Some(SetValue(value))
      }
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
      "scalafix:DisableSyntax.null",
      "scalafix:DisableSyntax.!="
    )
  )
  implicit def optionToNonNullableSetter[T: Manifest]
      : Transformer[Option[T], Option[NonNullableSetter[T]]] =
    new Transformer[Option[T], Option[NonNullableSetter[T]]] {
      override def transform(src: Option[T]): Option[NonNullableSetter[T]] = src match {
        case None => None
        case Some(map: Map[_, _]) if map.isEmpty =>
          // Workaround for CDF-3540 and CDF-953
          None
        case Some(value: T) =>
          require(
            value != null,
            "Invalid null value for non-nullable field update"
          ) // scalastyle:ignore null
          Some(SetValue(value))
      }
    }

  implicit def toNonNullableSetter[T: Manifest]: Transformer[T, NonNullableSetter[T]] =
    new Transformer[T, NonNullableSetter[T]] {
      override def transform(value: T): NonNullableSetter[T] = SetValue(value)
    }

  implicit def toOptionNonNullableSetter[T: Manifest]
      : Transformer[T, Option[NonNullableSetter[T]]] =
    new Transformer[T, Option[NonNullableSetter[T]]] {
      override def transform(value: T): Option[NonNullableSetter[T]] = Some(SetValue(value))
    }

  implicit def encodeNonNullableSetter[T](
      implicit encodeT: Encoder[T]
  ): Encoder[NonNullableSetter[T]] = new Encoder[NonNullableSetter[T]] {
    final def apply(a: NonNullableSetter[T]): Json = a match {
      case SetValue(value) => Json.obj(("set", encodeT.apply(value)))
    }
  }
}
