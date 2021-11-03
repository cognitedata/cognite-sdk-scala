// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1

import com.cognite.sdk.scala.common.{AggregateDataPoint, DataPoint, StringDataPoint}
import io.circe.syntax.EncoderOps
import io.circe.{Encoder, Json}

final case class DataPointsByExternalId(
    externalId: String,
    datapoints: Seq[DataPoint]
)

trait DataPointsResponse[D] {
  def id: Long
  def getExternalId: Option[String]
  def isString: Boolean
  def unit: Option[String]
  def datapoints: Seq[D]
}

final case class DataPointsByIdResponse(
    id: Long,
    externalId: Option[String],
    isString: Boolean,
    isStep: Boolean = true, // default value for accidental string points read
    unit: Option[String],
    datapoints: Seq[DataPoint]
) extends DataPointsResponse[DataPoint] {
  override def getExternalId: Option[String] = externalId
}

final case class DataPointsByExternalIdResponse(
    id: Long,
    externalId: String,
    isString: Boolean,
    isStep: Boolean = true, // default value for accidental string points read
    unit: Option[String],
    datapoints: Seq[DataPoint]
) extends DataPointsResponse[DataPoint] {
  override def getExternalId: Option[String] = Some(externalId)
}

final case class StringDataPointsByIdResponse(
    id: Long,
    externalId: Option[String],
    isString: Boolean,
    unit: Option[String],
    datapoints: Seq[StringDataPoint]
) extends DataPointsResponse[StringDataPoint] {
  override def getExternalId: Option[String] = externalId
}

final case class StringDataPointsByExternalIdResponse(
    id: Long,
    externalId: String,
    isString: Boolean,
    unit: Option[String],
    datapoints: Seq[StringDataPoint]
) extends DataPointsResponse[StringDataPoint] {
  override def getExternalId: Option[String] = Some(externalId)
}

final case class StringDataPointsByExternalId(
    externalId: String,
    datapoints: Seq[StringDataPoint]
)

final case class DeleteDataPointsRange(
    id: CogniteId,
    inclusiveBegin: Long,
    exclusiveEnd: Long
)
object DeleteDataPointsRange {
  implicit val encoder: Encoder[DeleteDataPointsRange] = Encoder.instance(v =>
    Json.obj(
      "inclusiveBegin" -> Json.fromLong(v.inclusiveBegin),
      "exclusiveEnd" -> Json.fromLong(v.exclusiveEnd),
      v.id match {
        case CogniteExternalId(externalId) => "externalId" -> Json.fromString(externalId)
        case CogniteInternalId(id) => "id" -> Json.fromLong(id)
      }
    )
  )
}

final case class LatestBeforeRequest(
    before: String,
    id: CogniteId
)
object LatestBeforeRequest {
  implicit val encoder: Encoder[LatestBeforeRequest] = Encoder.instance(v =>
    Json.obj(
      "before" -> Json.fromString(v.before),
      v.id match {
        case CogniteExternalId(externalId) => "externalId" -> Json.fromString(externalId)
        case CogniteInternalId(id) => "id" -> Json.fromLong(id)
      }
    )
  )
}

final case class QueryDataPointsRange(
    id: CogniteId,
    start: String,
    end: String,
    limit: Option[Int] = None,
    granularity: Option[String] = None,
    aggregates: Option[Seq[String]] = None
)
object QueryDataPointsRange {
  implicit val encoder: Encoder[QueryDataPointsRange] = Encoder.instance(v =>
    Json.obj(
      "start" -> Json.fromString(v.start),
      "end" -> Json.fromString(v.end),
      "limit" -> v.limit.asJson,
      "granularity" -> v.granularity.asJson,
      "aggregates" -> v.aggregates.asJson,
      v.id match {
        case CogniteExternalId(externalId) => "externalId" -> Json.fromString(externalId)
        case CogniteInternalId(id) => "id" -> Json.fromLong(id)
      }
    )
  )
}

final case class QueryAggregatesResponse(
    id: Long,
    externalId: Option[String],
    isString: Boolean,
    isStep: Boolean,
    unit: Option[String],
    datapoints: Seq[AggregateDataPoint]
)
