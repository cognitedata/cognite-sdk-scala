package com.cognite.sdk.scala.common

import com.softwaremill.sttp.Uri
import io.circe.{Decoder, Json, JsonObject}
import io.scalaland.chimney.dsl._

final case class ItemsWithCursor[A](items: Seq[A], nextCursor: Option[String] = None)
final case class Items[A](items: Seq[A])
final case class CdpApiErrorPayload[A](
    code: Int,
    message: String,
    missing: Option[Seq[A]],
    duplicated: Option[Seq[A]],
    missingFields: Option[Seq[Map[String, String]]]
)
final case class CdpApiError[A](error: CdpApiErrorPayload[A]) {
  def asException(url: Uri): CdpApiException[A] =
    this.error
      .into[CdpApiException[A]]
      .withFieldConst(_.url, url)
      .transform
}
final case class CdpApiException[A](
    url: Uri,
    code: Int,
    message: String,
    missing: Option[Seq[A]],
    duplicated: Option[Seq[A]]
) extends Throwable(s"Request to ${url.toString()} failed with status $code: $message")
final case class CogniteId(id: Long)

final case class DataPoint(
    timestamp: Long,
    value: Double
)

final case class StringDataPoint(
    timestamp: Long,
    value: String
)

trait WithId[I] {
  val id: I
}

trait Extractor[C[_]] {
  def extract[A](c: C[A]): A
}

object EitherDecoder {
  def eitherDecoder[A, B](implicit a: Decoder[A], b: Decoder[B]): Decoder[Either[A, B]] = {
    val l: Decoder[Either[A, B]] = a.map(Left.apply)
    val r: Decoder[Either[A, B]] = b.map(Right.apply)
    l.or(r)
  }
}

object ToUpdate {
  def apply(json: Json): Json = {
    if (!json.isObject) {
      throw new IllegalArgumentException(
        s"Only JSON objects can be converted to updates, got $json"
      )
    }
    json
      .mapObject { o =>
        // TODO: check id and externalId?
//        val updateId = o("id") match {
//          case Some(id) if id.isNumber => id
//          case Some(id) =>
//            throw new IllegalArgumentException(s"Invalid id ${id.toString()} for update")
//          case None =>
//            throw new IllegalArgumentException(s"Missing required field 'id' for update in $json")
//        }
        o.keys
          .map { key =>
            (key, o(key))
          }
          .foldLeft(JsonObject()) {
            // skip id field
            // TODO: skip externalId if we're updating based on that.
            case (updateValue, ("id", _)) =>
              updateValue
            case (updateValue, (key, Some(value))) =>
              val setValue = if (value.isNull) {
                JsonObject("setNull" -> Json.fromBoolean(true))
              } else {
                JsonObject("set" -> value)
              }
              updateValue.add(key, Json.fromJsonObject(setValue))
          }
      }
  }
}
