package com.cognite.sdk.scala.v1.resources

import com.cognite.sdk.scala.common._
import com.cognite.sdk.scala.v1.{CreateTimeSeries, TimeSeries, TimeSeriesQuery, TimeSeriesUpdate}
import com.softwaremill.sttp._

class TimeSeriesResource[F[_]](val requestSession: RequestSession)
    extends WithRequestSession
    with Readable[TimeSeries, F, Id]
    with RetrieveByIds[TimeSeries, F, Id]
    with Create[TimeSeries, CreateTimeSeries, F, Id]
    with DeleteByIdsV1[TimeSeries, CreateTimeSeries, F, Id]
    with DeleteByExternalIdsV1[F]
    with Search[TimeSeries, TimeSeriesQuery, F, Id]
    with Update[TimeSeries, TimeSeriesUpdate, F, Id] {
  override val baseUri = uri"${requestSession.baseUri}/timeseries"
}
