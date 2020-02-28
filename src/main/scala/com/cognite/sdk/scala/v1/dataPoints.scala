package com.cognite.sdk.scala.v1

import com.cognite.sdk.scala.common.{AggregateDataPoint, DataPoint, StringDataPoint}

final case class DataPointsByExternalId(
    externalId: String,
    datapoints: Seq[DataPoint]
)

final case class DataPointsByIdResponse(
    id: Long,
    externalId: Option[String],
    isString: Boolean,
    isStep: Boolean,
    unit: Option[String],
    datapoints: Seq[DataPoint]
)

final case class DataPointsByExternalIdResponse(
    id: Long,
    externalId: String,
    isString: Boolean,
    isStep: Boolean,
    unit: Option[String],
    datapoints: Seq[DataPoint]
)

final case class StringDataPointsByIdResponse(
    id: Long,
    externalId: Option[String],
    isString: Boolean,
    isStep: Option[Boolean],
    unit: Option[String],
    datapoints: Seq[StringDataPoint]
)

final case class StringDataPointsByExternalIdResponse(
    id: Long,
    externalId: String,
    isString: Boolean,
    isStep: Option[Boolean],
    unit: Option[String],
    datapoints: Seq[StringDataPoint]
)

final case class StringDataPointsByExternalId(
    externalId: String,
    datapoints: Seq[StringDataPoint]
)

sealed trait DeleteDataPointsRange

final case class DeleteRangeById(
    id: Long,
    inclusiveBegin: Long,
    exclusiveEnd: Long
) extends DeleteDataPointsRange

final case class DeleteRangeByExternalId(
    externalId: String,
    inclusiveBegin: Long,
    exclusiveEnd: Long
) extends DeleteDataPointsRange

final case class QueryRangeById(
    id: Long,
    start: String,
    end: String,
    limit: Option[Int] = None,
    granularity: Option[String] = None,
    aggregates: Option[Seq[String]] = None
)

final case class QueryRangeByExternalId(
    externalId: String,
    start: String,
    end: String,
    limit: Option[Int] = None,
    granularity: Option[String] = None,
    aggregates: Option[Seq[String]] = None
)

final case class QueryAggregatesResponse(
    id: Long,
    externalId: Option[String],
    isString: Boolean,
    isStep: Boolean,
    unit: Option[String],
    datapoints: Seq[AggregateDataPoint]
)
