package com.cognite.sdk.scala.v0_6

import com.cognite.sdk.scala.common.{Auth, WithId}
import com.softwaremill.sttp._
import io.circe.generic.auto._

final case class TimeSeries(
    name: String,
    isString: Boolean = false,
    metadata: Option[Map[String, String]] = None,
    unit: Option[String] = None,
    assetId: Option[Long] = None,
    isStep: Boolean = false,
    description: Option[String] = None,
    securityCategories: Option[Seq[Long]] = None,
    id: Long = 0,
    createdTime: Option[Long] = None,
    lastUpdatedTime: Option[Long] = None
) extends WithId[Long]

final case class CreateTimeSeries(
    name: String,
    isString: Boolean = false,
    metadata: Option[Map[String, String]] = None,
    unit: Option[String] = None,
    assetId: Option[Long] = None,
    isStep: Boolean = false,
    description: Option[String] = None,
    securityCategories: Option[Seq[Long]] = None
)

class TimeSeriesResourceRead[F[_]](implicit auth: Auth, sttpBackend: SttpBackend[F, _])
    extends ReadWritableResourceV0_6[TimeSeries, CreateTimeSeries, F]
    with ResourceV0_6[F] {
  override val baseUri = uri"https://api.cognitedata.com/api/0.6/projects/playground/timeseries"
}
