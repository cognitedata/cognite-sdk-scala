// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1

import java.time.Instant
import com.cognite.sdk.scala.common._

//import scala.annotation.nowarn

final case class Asset(
    externalId: Option[String] = None,
    name: String,
    parentId: Option[Long] = None,
    description: Option[String] = None,
    metadata: Option[Map[String, String]] = None,
    source: Option[String] = None,
    id: Long = 0,
    createdTime: Instant = Instant.ofEpochMilli(0),
    lastUpdatedTime: Instant = Instant.ofEpochMilli(0),
    rootId: Option[Long] = None,
    aggregates: Option[Map[String, Long]] = None,
    dataSetId: Option[Long] = None,
    parentExternalId: Option[String] = None,
    labels: Option[Seq[CogniteExternalId]] = None
) extends WithId[Long]
    with WithExternalId
    with WithCreatedTime
    with ToCreate[AssetCreate]
    with ToUpdate[AssetUpdate] {
  override def toCreate: AssetCreate =
    AssetCreate(
      name,
      parentId,
      description,
      source,
      externalId,
      metadata,
      parentExternalId,
      dataSetId,
      labels
    )

  override def toUpdate: AssetUpdate =
    AssetUpdate(
      Some(NonNullableSetter.fromAny(name)),
      Setter.fromOption(description),
      Setter.fromOption(source),
      Setter.fromOption(externalId),
      NonNullableSetter.fromOption(metadata),
      Setter.fromOption(parentId),
      Setter.fromOption(parentExternalId),
      Setter.fromOption(dataSetId),
      NonNullableSetter.fromOption(labels)
    )
}

final case class AssetCreate(
    name: String,
    parentId: Option[Long] = None,
    description: Option[String] = None,
    source: Option[String] = None,
    externalId: Option[String] = None,
    metadata: Option[Map[String, String]] = None,
    parentExternalId: Option[String] = None,
    dataSetId: Option[Long] = None,
    labels: Option[Seq[CogniteExternalId]] = None
) extends WithExternalId

final case class AssetUpdate(
    name: Option[NonNullableSetter[String]] = None,
    description: Option[Setter[String]] = None,
    source: Option[Setter[String]] = None,
    externalId: Option[Setter[String]] = None,
    metadata: Option[NonNullableSetter[Map[String, String]]] = None,
    parentId: Option[Setter[Long]] = None,
    parentExternalId: Option[Setter[String]] = None,
    dataSetId: Option[Setter[Long]] = None,
    labels: Option[NonNullableSetter[Seq[CogniteExternalId]]] = None
) extends WithSetExternalId

//@nowarn
final case class AssetsFilter(
    name: Option[String] = None,
    parentIds: Option[Seq[Long]] = None,
//    @deprecated("Use assetSubtreeIds instead", "2.1.0")
    rootIds: Option[Seq[CogniteId]] = None,
    assetSubtreeIds: Option[Seq[CogniteId]] = None,
    metadata: Option[Map[String, String]] = None,
    source: Option[String] = None,
    createdTime: Option[TimeRange] = None,
    lastUpdatedTime: Option[TimeRange] = None,
    root: Option[Boolean] = None,
    externalIdPrefix: Option[String] = None,
    dataSetIds: Option[Seq[CogniteId]] = None,
    parentExternalIds: Option[Seq[String]] = None,
    labels: Option[LabelContainsFilter] = None
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
