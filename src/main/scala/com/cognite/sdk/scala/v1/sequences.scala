package com.cognite.sdk.scala.v1

import java.time.Instant

import cats.data.NonEmptyList
import com.cognite.sdk.scala.common.{NonNullableSetter, SearchQuery, Setter, WithExternalId, WithId}

final case class SequenceColumn(
    id: Long,
    name: Option[String] = None,
    externalId: Option[String] = None,
    description: Option[String] = None,
    // TODO: Turn this into an enum.
    //       See https://github.com/circe/circe-derivation/issues/8
    //       and https://github.com/circe/circe-derivation/pull/91
    valueType: String = "STRING",
    metadata: Option[Map[String, String]] = None,
    createdTime: Instant,
    lastUpdatedTime: Instant
)

final case class Sequence(
    id: Long = 0,
    name: Option[String] = None,
    description: Option[String] = None,
    assetId: Option[Long] = None,
    externalId: Option[String] = None,
    metadata: Option[Map[String, String]] = None,
    columns: NonEmptyList[SequenceColumn],
    createdTime: Instant = Instant.ofEpochMilli(0),
    lastUpdatedTime: Instant = Instant.ofEpochMilli(0)
) extends WithId[Long]
    with WithExternalId

final case class SequenceCreate(
    name: Option[String] = None,
    description: Option[String] = None,
    assetId: Option[Long] = None,
    externalId: Option[String] = None,
    metadata: Option[Map[String, String]] = None,
    columns: NonEmptyList[SequenceColumn]
) extends WithExternalId

final case class SequenceUpdate(
    id: Long = 0,
    name: Option[Setter[String]] = None,
    description: Option[Setter[String]] = None,
    assetId: Option[Setter[Long]] = None,
    externalId: Option[Setter[String]] = None,
    metadata: Option[NonNullableSetter[Map[String, String]]] = None
) extends WithId[Long]

final case class SequenceFilter(
    name: Option[String] = None,
    externalIdPrefix: Option[String] = None,
    metadata: Option[Map[String, String]] = None,
    assetIds: Option[Seq[Long]] = None,
    rootAssetIds: Option[Seq[Long]] = None,
    createdTime: Option[TimeRange] = None,
    lastUpdatedTime: Option[TimeRange] = None
)

final case class SequenceSearch(
    name: Option[String] = None,
    description: Option[String] = None,
    query: Option[String] = None
)

final case class SequenceQuery(
    filter: Option[SequenceFilter] = None,
    search: Option[SequenceSearch] = None,
    limit: Int = 100
) extends SearchQuery[SequenceFilter, SequenceSearch]
