// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1

import java.time.Instant
import com.cognite.sdk.scala.common._

final case class TimeSeries(
    name: Option[String] = None,
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
    lastUpdatedTime: Instant = Instant.ofEpochMilli(0),
    dataSetId: Option[Long] = None
) extends WithId[Long]
    with WithExternalId
    with WithCreatedTime
    with ToCreate[TimeSeriesCreate]
    with ToUpdate[TimeSeriesUpdate] {
  override def toCreate: TimeSeriesCreate =
    TimeSeriesCreate(
      externalId,
      name,
      isString,
      metadata,
      unit,
      assetId,
      isStep,
      description,
      securityCategories,
      dataSetId
    )

  override def toUpdate: TimeSeriesUpdate =
    TimeSeriesUpdate(
      Setter.fromOption(name),
      Setter.fromOption(externalId),
      NonNullableSetter.fromOption(metadata),
      Setter.fromOption(unit),
      Setter.fromOption(assetId),
      Setter.fromOption(description),
      NonNullableSetter.fromOption(securityCategories),
      Setter.fromOption(dataSetId)
    )
}

final case class TimeSeriesCreate(
    externalId: Option[String] = None,
    name: Option[String] = None,
    isString: Boolean = false,
    metadata: Option[Map[String, String]] = None,
    unit: Option[String] = None,
    assetId: Option[Long] = None,
    isStep: Boolean = false,
    description: Option[String] = None,
    securityCategories: Option[Seq[Long]] = None,
    dataSetId: Option[Long] = None
) extends WithExternalId

final case class TimeSeriesUpdate(
    name: Option[Setter[String]] = None,
    externalId: Option[Setter[String]] = None,
    metadata: Option[NonNullableSetter[Map[String, String]]] = None,
    unit: Option[Setter[String]] = None,
    assetId: Option[Setter[Long]] = None,
    description: Option[Setter[String]] = None,
    securityCategories: Option[NonNullableSetter[Seq[Long]]] = None,
    dataSetId: Option[Setter[Long]] = None
) extends WithSetExternalId

final case class TimeSeriesSearchFilter(
    name: Option[String] = None,
    unit: Option[String] = None,
    isString: Option[Boolean] = None,
    isStep: Option[Boolean] = None,
    metadata: Option[Map[String, String]] = None,
    assetIds: Option[Seq[Long]] = None,
    externalIdPrefix: Option[String] = None,
    createdTime: Option[TimeRange] = None,
    lastUpdatedTime: Option[TimeRange] = None,
    dataSetIds: Option[Seq[CogniteId]] = None
)

final case class TimeSeriesFilter(
    name: Option[String] = None,
    unit: Option[String] = None,
    isString: Option[Boolean] = None,
    isStep: Option[Boolean] = None,
    metadata: Option[Map[String, String]] = None,
    assetIds: Option[Seq[Long]] = None,
    rootAssetIds: Option[Seq[CogniteId]] = None,
    externalIdPrefix: Option[String] = None,
    createdTime: Option[TimeRange] = None,
    lastUpdatedTime: Option[TimeRange] = None,
    dataSetIds: Option[Seq[CogniteId]] = None
)

final case class TimeSeriesSearch(
    name: Option[String] = None,
    description: Option[String] = None,
    query: Option[String] = None
)

final case class TimeSeriesQuery(
    filter: Option[TimeSeriesSearchFilter] = None,
    search: Option[TimeSeriesSearch] = None,
    limit: Int = 100
) extends SearchQuery[TimeSeriesSearchFilter, TimeSeriesSearch]

final case class SyntheticTimeSeriesQuery(
    expression: String,
    start: Instant,
    end: Instant,
    limit: Int
)

final case class SyntheticTimeSeriesResponse(
    isString: Boolean,
    datapoints: Seq[DataPoint]
)
