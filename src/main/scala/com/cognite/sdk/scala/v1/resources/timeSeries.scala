package com.cognite.sdk.scala.v1.resources

import com.cognite.sdk.scala.common.Auth
import com.cognite.sdk.scala.v1.{CreateTimeSeries, TimeSeries}
import com.softwaremill.sttp._
import io.circe.generic.auto._

class TimeSeriesResource[F[_]](project: String)(
    implicit auth: Auth
) extends ReadWritableResourceV1[TimeSeries, CreateTimeSeries, F]
    with ResourceV1[F] {
  override val baseUri = uri"https://api.cognitedata.com/api/v1/projects/$project/timeseries"
}
