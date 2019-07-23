package com.cognite.sdk.scala.v1.resources

import com.cognite.sdk.scala.common._
import com.cognite.sdk.scala.v1.{CreateTimeSeries, TimeSeries, TimeSeriesQuery, TimeSeriesUpdate}
import com.softwaremill.sttp._

class TimeSeriesResource[F[_]](val requestSession: RequestSession)
    extends WithRequestSession
    with Readable[TimeSeries, F]
    with RetrieveByIds[TimeSeries, F]
    with Create[TimeSeries, CreateTimeSeries, F]
    with DeleteByIdsV1[TimeSeries, CreateTimeSeries, F]
    with DeleteByExternalIdsV1[F]
    with Search[TimeSeries, TimeSeriesQuery, F]
    with Update[TimeSeries, TimeSeriesUpdate, F] {
  override val baseUri = uri"${requestSession.baseUri}/timeseries"
}
