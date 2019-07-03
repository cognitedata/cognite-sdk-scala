package com.cognite.sdk.scala.v1

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
    externalId: Option[String] = None,
    createdTime: Long = 0,
    lastUpdatedTime: Long = 0
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

class TimeSeriesResource[F[_]](project: String)(
    implicit auth: Auth,
    sttpBackend: SttpBackend[F, _]
) extends ReadWritableResourceV1[TimeSeries, CreateTimeSeries, F]
    with ResourceV1[F] {
  override val baseUri = uri"https://api.cognitedata.com/api/v1/projects/$project/timeseries"
}
