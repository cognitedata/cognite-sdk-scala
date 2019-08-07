package com.cognite.sdk.scala.v1

import java.time.Instant

import com.cognite.sdk.scala.common.{NonNullableSetter, SearchQuery, Setter, WithExternalId, WithId}

final case class Asset(
    externalId: Option[String] = None,
    name: String,
    parentId: Option[Long] = None,
    description: Option[String] = None,
    metadata: Option[Map[String, String]] = None,
    source: Option[String] = None,
    id: Long = 0,
    createdTime: Instant = Instant.ofEpochMilli(0),
    lastUpdatedTime: Instant = Instant.ofEpochMilli(0)
) extends WithId[Long]
    with WithExternalId

final case class AssetCreate(
    name: String,
    parentId: Option[Long] = None,
    description: Option[String] = None,
    source: Option[String] = None,
    externalId: Option[String] = None,
    metadata: Option[Map[String, String]] = None,
    parentExternalId: Option[String] = None
) extends WithExternalId

final case class AssetUpdate(
    id: Long,
    name: Option[NonNullableSetter[String]] = None,
    description: Option[Setter[String]] = None,
    source: Option[Setter[String]] = None,
    externalId: Option[Setter[String]] = None,
    metadata: Option[NonNullableSetter[Map[String, String]]] = None
) extends WithId[Long]

final case class AssetsFilter(
    name: Option[String] = None,
    parentIds: Option[Seq[Long]] = None,
    metadata: Option[Map[String, String]] = None,
    source: Option[String] = None,
    createdTime: Option[TimeRange] = None,
    lastUpdatedTime: Option[TimeRange] = None,
    root: Option[Boolean] = None,
    externalIdPrefix: Option[String] = None
)

final case class AssetsSearch(
    name: Option[String] = None,
    description: Option[String] = None
)

final case class AssetsQuery(
    filter: Option[AssetsFilter] = None,
    search: Option[AssetsSearch] = None,
    limit: Int = 100
) extends SearchQuery[AssetsFilter, AssetsSearch]
