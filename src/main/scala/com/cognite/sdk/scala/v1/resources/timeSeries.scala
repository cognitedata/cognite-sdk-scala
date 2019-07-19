package com.cognite.sdk.scala.v1.resources

import com.cognite.sdk.scala.common.{RequestSession, Search, Update, WithRequestSession}
import com.cognite.sdk.scala.v1.{TimeSeries, TimeSeriesQuery, TimeSeriesUpdate}
import com.softwaremill.sttp._
import io.circe.generic.auto._

class TimeSeriesResource[F[_]](val requestSession: RequestSession)
    extends WithRequestSession
    with Search[TimeSeries, TimeSeriesQuery, F, Id]
    with Update[TimeSeries, TimeSeriesUpdate, F, Id] {
  override val baseUri = uri"${requestSession.baseUri}/timeseries"
}
