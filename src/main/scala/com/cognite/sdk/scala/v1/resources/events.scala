package com.cognite.sdk.scala.v1.resources

import com.cognite.sdk.scala.common._
import com.cognite.sdk.scala.v1.{CreateEvent, Event, EventUpdate, EventsFilter, EventsQuery}
import com.softwaremill.sttp._

class Events[F[_]](val requestSession: RequestSession)
    extends WithRequestSession
    with Readable[Event, F, Id]
    with RetrieveByIds[Event, F, Id]
    with Create[Event, CreateEvent, F, Id]
    with DeleteByIdsV1[Event, CreateEvent, F, Id]
    with DeleteByExternalIdsV1[F]
    with Filter[Event, EventsFilter, F, Id]
    with Search[Event, EventsQuery, F, Id]
    with Update[Event, EventUpdate, F, Id] {
  override val baseUri = uri"${requestSession.baseUri}/events"
}
