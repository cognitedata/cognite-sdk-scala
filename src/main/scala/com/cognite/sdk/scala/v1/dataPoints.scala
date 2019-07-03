package com.cognite.sdk.scala.v1

import com.cognite.sdk.scala.common.{DataPoint, StringDataPoint}

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
