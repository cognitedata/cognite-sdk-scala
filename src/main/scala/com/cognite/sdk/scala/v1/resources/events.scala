package com.cognite.sdk.scala.v1.resources

import com.cognite.sdk.scala.common._
import com.cognite.sdk.scala.v1.{CreateEvent, Event, EventUpdate, EventsFilter, EventsQuery, RequestSession}
import com.softwaremill.sttp._

class Events[F[_]](val requestSession: RequestSession[F])
    extends WithRequestSession[F]
    with Readable[Event, F]
    with RetrieveByIds[Event, F]
    with Create[Event, CreateEvent, F]
    with DeleteByIdsV1[Event, CreateEvent, F]
    with DeleteByExternalIdsV1[F]
    with Filter[Event, EventsFilter, F]
    with Search[Event, EventsQuery, F]
    with Update[Event, EventUpdate, F] {
  override val baseUri = uri"${requestSession.baseUri}/events"
}
