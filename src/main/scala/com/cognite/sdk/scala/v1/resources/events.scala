package com.cognite.sdk.scala.v1.resources

import com.cognite.sdk.scala.common.{Auth, Update, Search}
import com.cognite.sdk.scala.v1.{CreateEvent, Event, EventsQuery, EventUpdate}
import com.softwaremill.sttp._
import io.circe.generic.auto._

class Events[F[_]](project: String)(implicit auth: Auth)
    extends ReadWritableResourceV1[Event, CreateEvent, F]
    with ResourceV1[F]
    with Search[Event, EventsQuery, F, Id]
    with Update[Event, EventUpdate, F, Id] {
  override val baseUri = uri"https://api.cognitedata.com/api/v1/projects/$project/events"
}
