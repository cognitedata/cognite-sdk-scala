package com.cognite.sdk.scala.v1

import java.time.Instant

import com.cognite.sdk.scala.common.{NonNullableSetter, SearchQuery, Setter, WithExternalId, WithId}

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
    createdTime: Instant = Instant.ofEpochMilli(0),
    lastUpdatedTime: Instant = Instant.ofEpochMilli(0)
) extends WithId[Long]
    with WithExternalId

final case class CreateTimeSeries(
    externalId: Option[String] = None,
    name: String,
    legacyName: Option[String] = None,
    isString: Boolean = false,
    metadata: Option[Map[String, String]] = None,
    unit: Option[String] = None,
    assetId: Option[Long] = None,
    isStep: Boolean = false,
    description: Option[String] = None,
    securityCategories: Option[Seq[Long]] = None
) extends WithExternalId

final case class TimeSeriesUpdate(
    id: Long = 0,
    name: Option[Setter[String]] = None,
    externalId: Option[Setter[String]] = None,
    metadata: Option[NonNullableSetter[Map[String, String]]] = None,
    unit: Option[Setter[String]] = None,
    assetId: Option[Setter[Long]] = None,
    description: Option[Setter[String]] = None,
    securityCategories: Option[Setter[Seq[Long]]] = None
) extends WithId[Long]

final case class TimeSeriesFilter(
    name: Option[String] = None,
    unit: Option[String] = None,
    isString: Option[Boolean] = None,
    isStep: Option[Boolean] = None,
    metadata: Option[Map[String, String]] = None,
    assetIds: Option[Seq[Long]] = None,
    externalIdPrefix: Option[String] = None,
    createdTime: Option[TimeRange] = None,
    lastUpdatedTime: Option[TimeRange] = None
)

final case class TimeSeriesSearch(
    name: Option[String] = None,
    description: Option[String] = None,
    query: Option[String] = None
)

final case class TimeSeriesQuery(
    filter: Option[TimeSeriesFilter] = None,
    search: Option[TimeSeriesSearch] = None,
    limit: Int = 100
) extends SearchQuery[TimeSeriesFilter, TimeSeriesSearch]
