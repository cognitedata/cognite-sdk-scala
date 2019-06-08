package com.cognite.sdk.scala.v1_0

import com.cognite.sdk.scala.common.{Auth, Items, ItemsWithCursor, WithId}
import com.softwaremill.sttp._
import io.circe.{Decoder, Encoder}

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
) extends WithId

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

class TimeSeriesResourceRead[F[_]](
    implicit val auth: Auth,
    val sttpBackend: SttpBackend[F, _],
    val readDecoder: Decoder[TimeSeries],
    val writeDecoder: Decoder[CreateTimeSeries],
    val writeEncoder: Encoder[CreateTimeSeries],
    val containerItemsWithCursorDecoder: Decoder[Id[ItemsWithCursor[TimeSeries]]],
    val containerItemsDecoder: Decoder[Id[Items[TimeSeries]]]
) extends ReadWritableResourceV1[TimeSeries, CreateTimeSeries, F] with ResourceV1[F] {
  override val baseUri = uri"https://api.cognitedata.com/api/v1/projects/playground/timeseries"
}
