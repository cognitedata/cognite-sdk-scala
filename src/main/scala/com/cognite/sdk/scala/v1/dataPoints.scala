package com.cognite.sdk.scala.v1

import com.cognite.sdk.scala.common.{AggregateDataPoint, DataPoint, StringDataPoint}

final case class DataPointsById(
    id: Long,
    datapoints: Seq[DataPoint]
)

final case class DataPointsByIdResponse(
    id: Long,
    externalId: Option[String],
    isString: Boolean,
    datapoints: Seq[DataPoint]
)

final case class StringDataPointsByIdResponse(
    id: Long,
    externalId: Option[String],
    isString: Boolean,
    datapoints: Seq[StringDataPoint]
)

final case class StringDataPointsById(
    id: Long,
    datapoints: Seq[StringDataPoint]
)

final case class DeleteRangeById(
    id: Long,
    inclusiveBegin: Long,
    exclusiveEnd: Long
)

final case class DeleteRangeByExternalId(
    id: String,
    inclusiveBegin: Long,
    exclusiveEnd: Long
)

final case class QueryRangeById(
    id: Long,
    start: String,
    end: String
)

final case class QueryRangeByExternalId(
    id: String,
    start: String,
    end: String
                                       )

final case class QueryAggregatesById(
    id: Long,
    start: String,
    end: String,
    granularity: String,
    aggregates: Seq[String]
)

final case class QueryAggregatesByIdResponse(
    id: Long,
    externalId: Option[String],
    datapoints: Seq[AggregateDataPoint]
)

final case class QueryAggregatesByExternalId(
    externalId: String,
    start: String,
    end: String,
    granularity: String,
    aggregates: Seq[String]
)
