package com.cognite.sdk.scala.v1_0

import com.cognite.sdk.scala.common.{Auth, WithId}
import com.softwaremill.sttp._
import io.circe.generic.auto._

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
    externalId: Option[String] = None,
    createdTime: Long = 0,
    lastUpdatedTime: Long = 0
) extends WithId

final case class CreateEvent(
    startTime: Option[Long] = None,
    endTime: Option[Long] = None,
    description: Option[String] = None,
    `type`: Option[String] = None,
    subtype: Option[String] = None,
    metadata: Option[Map[String, String]] = None,
    assetIds: Option[Seq[Long]] = None,
    source: Option[String] = None,
    externalId: Option[String] = None
)

class Events[F[_]](
    implicit val auth: Auth,
    val sttpBackend: SttpBackend[F, _],
) extends ReadWritableResourceV1[Event, CreateEvent, F] with ResourceV1[F] {
  override val baseUri = uri"https://api.cognitedata.com/api/v1/projects/playground/events"
}
