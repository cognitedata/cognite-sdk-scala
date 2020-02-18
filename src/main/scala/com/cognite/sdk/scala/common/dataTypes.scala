package com.cognite.sdk.scala.common

import java.time.Instant

import com.cognite.sdk.scala.v1.CogniteId
import com.softwaremill.sttp.Uri
import io.circe.{Decoder, Encoder, Json, JsonObject}
import io.circe.derivation.deriveDecoder
import io.scalaland.chimney.Transformer
import io.scalaland.chimney.dsl._

final case class ItemsWithCursor[A](items: Seq[A], nextCursor: Option[String] = None)
final case class Items[A](items: Seq[A])
final case class ItemsWithIgnoreUnknownIds(items: Seq[CogniteId], ignoreUnknownIds: Boolean)
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
) extends Throwable(message) {
  override def toString: String = {
    val responseCodeMessage = responseCode
      .map(c => s" (with response code ${c.toString})")
      .getOrElse("")
    val uriMessage = uri
      .map(u => s", in response$responseCodeMessage to request sent to ${u.toString()}")
      .getOrElse(responseCodeMessage)
    val requestIdMessage = requestId.map(id => s", with request id $id").getOrElse("")
    val superString = super.toString
    val superMessage = if (superString.length > 0) {
      superString
    } else {
      "Missing error message"
    }
    s"$superMessage$uriMessage$requestIdMessage"
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
) extends Throwable(
      s"Request ${requestId.map(id => s"with id $id ").getOrElse("")}to ${url
        .toString()} failed with status ${code.toString}: $message"
    )

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

trait WithExternalId {
  val externalId: Option[String]
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
        case Some(value: T) => Some(SetValue(value))
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
        case Some(value: T) =>
          require(value != null, "Invalid null value for non-nullable field update") // scalastyle:ignore null
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
