package com.cognite.sdk.scala.v1.resources

import com.cognite.sdk.scala.common.{Auth, CogniteId, Filter, RequestSession, Search, Update, WithRequestSession}
import com.cognite.sdk.scala.v1.{CreateEvent, Event, EventUpdate, EventsFilter, EventsQuery}
import com.softwaremill.sttp._
import io.circe.generic.auto._

class Events[F[_]](val requestSession: RequestSession)(implicit auth: Auth)
    extends WithRequestSession
    with DeleteByIdsV1[Event, CreateEvent, F, Id]
    with DeleteByExternalIdsV1[F]
    with Filter[Event, EventsFilter, F, Id]
    with Search[Event, EventsQuery, F, Id]
    with Update[Event, EventUpdate, F, Id] {
  override val baseUri = uri"${requestSession.baseUri}/events"
}
