package com.cognite.sdk.scala.v1.resources

import com.cognite.sdk.scala.common.{Auth, Filter, Search, Update}
import com.cognite.sdk.scala.v1.{CreateEvent, Event, EventUpdate, EventsFilter, EventsQuery}
import com.softwaremill.sttp._
import io.circe.generic.auto._

class Events[F[_]](project: String)(implicit auth: Auth)
    extends ReadWritableResourceV1[Event, CreateEvent, F]
    with ResourceV1[F]
    with Filter[Event, EventsFilter, F, Id]
    with Search[Event, EventsQuery, F, Id]
    with Update[Event, EventUpdate, F, Id] {
  override val baseUri = uri"https://api.cognitedata.com/api/v1/projects/$project/events"
}
