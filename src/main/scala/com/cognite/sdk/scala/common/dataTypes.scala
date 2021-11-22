// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.common

import java.time.Instant
import com.cognite.sdk.scala.v1.{CogniteExternalId, CogniteId, CogniteInternalId}
import com.github.plokhotnyuk.jsoniter_scala.core.{JsonReader, JsonValueCodec, JsonWriter}
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker
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
    missing: Option[Seq[CogniteId]],
    duplicated: Option[Seq[CogniteId]],
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
  implicit val cdpApiErrorPayloadCodec: JsonValueCodec[CdpApiErrorPayload] = JsonCodecMaker.make
  implicit val cdpApiErrorCodec: JsonValueCodec[CdpApiError] = JsonCodecMaker.make
}

final case class CdpApiException(
    url: Uri,
    code: Int,
    message: String,
    missing: Option[Seq[CogniteId]],
    duplicated: Option[Seq[CogniteId]],
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
  private def describeErrorList(kind: String)(items: Seq[CogniteId]): String =
    items
      .groupBy(_.isInstanceOf[CogniteInternalId])
      .map { case (isInternalId, entries) =>
        // Example:
        //    Duplicated ids: [1234567, 1234568]. Duplicated externalIds: [externalId-1, externalId-2].

        val commaSeparatedValues =
          entries
            .map {
              case CogniteExternalId(externalId) => externalId
              case CogniteInternalId(id) => id.toString
            }
            .sorted // Ensure deterministic ordering
            .mkString(", ")
        val key = if (isInternalId) {
          "id"
        } else {
          "externalId"
        }
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

trait WithExternalIdGeneric[F[_]] extends WithGetExternalId {
  val externalId: F[String]
}

trait WithExternalId extends WithExternalIdGeneric[Option] {
  def getExternalId: Option[String] = externalId
}

trait WithRequiredExternalId extends WithExternalIdGeneric[Some] {
  def getExternalId: Option[String] = externalId
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
      case null => Some(SetNull()) // scalastyle:ignore null
      case None => None
      case Some(null) => Some(SetNull()) // scalastyle:ignore null
      case Some(map: Map[_, _]) if map.isEmpty =>
        // Workaround for CDF-3540 and CDF-953
        None
      case Some(value) => Some(SetValue(value))
    }

  @SuppressWarnings(Array("org.wartremover.warts.Null"))
  def fromAny[T](optionValue: T): Option[Setter[T]] =
    optionValue match {
      case null => Some(SetNull()) // scalastyle:ignore null
      case value => Some(SetValue(value))
    }

  implicit def encodeSetter[T](implicit encodeT : JsonValueCodec[T]): JsonValueCodec[Setter[T]] =
    new JsonValueCodec[Setter[T]] {
      override def decodeValue(in: JsonReader, default: Setter[T]): Setter[T] = ???

      override def encodeValue(x: Setter[T], out: JsonWriter): Unit = {
        out.writeObjectStart()
        x match {
          case SetValue(set) =>
            out.writeKey("set")
            encodeT.encodeValue(set, out)
          case SetNull() =>
            out.writeKey("setNull")
            out.writeVal(true)
        }
        out.writeObjectEnd()
      }

      override def nullValue: Setter[T] = null
    }
}

object NonNullableSetter {
  @SuppressWarnings(
    Array(
      "org.wartremover.warts.Null",
      "org.wartremover.warts.Equals",
      "org.wartremover.warts.OptionPartial"
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

  implicit def encodeNonNullableSetterSeq[T](
      implicit encodeT: JsonValueCodec[T]
  ): JsonValueCodec[NonNullableSetter[Seq[T]]] = new JsonValueCodec[NonNullableSetter[Seq[T]]] {
//    private val encodeSeqT = Codec.encodeSeq(encodeT)
//
//    final def apply(a: NonNullableSetter[Seq[T]]): Json = a match {
//      case SetValue(value) =>
//        Json.obj("set" -> encodeSeqT(value))
//      case UpdateArray(add: Seq[T] @unchecked, remove: Seq[T] @unchecked) =>
//        Json.obj("add" -> encodeSeqT(add), "remove" -> encodeSeqT(remove))
//    }

    override def decodeValue(in: JsonReader, default: NonNullableSetter[Seq[T]]): NonNullableSetter[Seq[T]] = ???

    override def encodeValue(x: NonNullableSetter[Seq[T]], out: JsonWriter): Unit = {
      out.writeObjectStart()
      x match {
        case SetValue(set) =>
          if (set.nonEmpty) {
            out.writeKey("set")
            out.writeArrayStart()
            set.foreach(encodeT.encodeValue(_, out))
            out.writeArrayEnd()
          }
        case UpdateArray(add, remove) =>
          if (add.nonEmpty) {
            out.writeKey("add")
            out.writeArrayStart()
            add.foreach(encodeT.encodeValue(_, out))
            out.writeArrayEnd()
          }
          if (remove.nonEmpty) {
            out.writeKey("remove")
            out.writeArrayStart()
            remove.foreach(encodeT.encodeValue(_, out))
            out.writeArrayEnd()
          }
      }
      out.writeObjectEnd()
    }

    override def nullValue: NonNullableSetter[Seq[T]] = null
  }

//  private val encodeMapStringString =
//    Codec.encodeMap[String, String](KeyCodec.encodeKeyString, Codec.encodeString)
//  private val encodeSeqString = Codec.encodeSeq[String]
  private val mapStringStringCodec: JsonValueCodec[Map[String, String]] = JsonCodecMaker.make
  private val seqStringCodec: JsonValueCodec[Seq[String]] = JsonCodecMaker.make
  implicit def encodeNonNullableSetterMapStringString
      : JsonValueCodec[NonNullableSetter[Map[String, String]]] =
    new JsonValueCodec[NonNullableSetter[Map[String, String]]] {
      override def decodeValue(in: JsonReader, default: NonNullableSetter[Map[String, String]]): NonNullableSetter[Map[String, String]] = ???

      override def encodeValue(x: NonNullableSetter[Map[String, String]], out: JsonWriter): Unit = {
        out.writeObjectStart()
        x match {
          case SetValue(set) =>
            out.writeKey("set")
            mapStringStringCodec.encodeValue(set, out)
          case UpdateMap(add, remove) =>
            out.writeKey("update")
            mapStringStringCodec.encodeValue(add, out)
            out.writeKey("remove")
            seqStringCodec.encodeValue(remove, out)
        }
        out.writeObjectEnd()
      }

      override def nullValue: NonNullableSetter[Map[String, String]] = null
    }

  implicit def encodeNonNullableSetter[T](
      implicit encodeT: JsonValueCodec[T]
  ): JsonValueCodec[NonNullableSetter[T]] = new JsonValueCodec[NonNullableSetter[T]] {
    override def decodeValue(in: JsonReader, default: NonNullableSetter[T]): NonNullableSetter[T] = ???

    override def encodeValue(x: NonNullableSetter[T], out: JsonWriter): Unit = {
      out.writeObjectStart()
      x match {
        case SetValue(value) =>
          out.writeKey("set")
          encodeT.encodeValue(value, out)
        case _ =>
          throw new RuntimeException("Invalid NonNullableSetter. This should never happen.")
      }
      out.writeObjectEnd()
    }

    override def nullValue: NonNullableSetter[T] = null
  }
}
