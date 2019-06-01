package com.cognite.sdk.scala.v1_0

import com.cognite.sdk.scala.common.{Auth, Items, ItemsWithCursor}
import com.softwaremill.sttp._
import io.circe.{Decoder, Encoder}

final case class Event(
    id: Long = 0,
    startTime: Option[Long] = None,
    endTime: Option[Long] = None,
    description: Option[String] = None,
    `type`: Option[String] = None,
    subtype: Option[String] = None,
    metadata: Option[Map[String, String]] = None,
    assetIds: Option[Seq[Long]] = None,
    source: Option[String] = None,
    sourceId: Option[String] = None,
    createdTime: Long = 0,
    lastUpdatedTime: Long = 0
)

final case class CreateEvent(
    startTime: Option[Long],
    endTime: Option[Long],
    description: Option[String],
    `type`: Option[String],
    subtype: Option[String],
    metadata: Option[Map[String, String]],
    assetIds: Option[Seq[Long]],
    source: Option[String],
    sourceId: Option[String]
)

class Events[F[_]](
    implicit val auth: Auth,
    val sttpBackend: SttpBackend[F, _],
    val readDecoder: Decoder[Event],
    val writeDecoder: Decoder[CreateEvent],
    val writeEncoder: Encoder[CreateEvent],
    val containerItemsWithCursorDecoder: Decoder[Id[ItemsWithCursor[Event]]],
    val containerItemsDecoder: Decoder[Id[Items[Event]]]
) extends ResourceV1[F]
    with ReadableResourceV1[Event, F]
    with WritableResourceV1[Event, CreateEvent, F] {
  override val baseUri = uri"https://api.cognitedata.com/api/0.6/projects/playground/events"
}
