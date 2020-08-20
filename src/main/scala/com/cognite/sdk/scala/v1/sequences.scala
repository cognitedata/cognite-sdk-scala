package com.cognite.sdk.scala.v1

import java.time.Instant

import cats.data.NonEmptyList
import com.cognite.sdk.scala.common.{
  NonNullableSetter,
  SearchQuery,
  Setter,
  WithCreatedTime,
  WithExternalId,
  WithId,
  WithSetExternalId
}

final case class SequenceColumn(
    name: Option[String] = None,
    // externalId must be optional until all data created using v0.6
    // of the API has been migrated and this field has been marked as
    // required in the official API docs.
    // See also our custom transformer in common/package.scala
    externalId: Option[String] = None,
    description: Option[String] = None,
    // TODO: Turn this into an enum.
    //       See https://github.com/circe/circe-derivation/issues/8
    //       and https://github.com/circe/circe-derivation/pull/91
    valueType: String = "STRING",
    metadata: Option[Map[String, String]] = None,
    createdTime: Instant = Instant.ofEpochMilli(0),
    lastUpdatedTime: Instant = Instant.ofEpochMilli(0)
)

final case class SequenceColumnCreate(
    name: Option[String] = None,
    externalId: String,
    description: Option[String] = None,
    // TODO: Turn this into an enum.
    //       See https://github.com/circe/circe-derivation/issues/8
    //       and https://github.com/circe/circe-derivation/pull/91
    valueType: String = "STRING",
    metadata: Option[Map[String, String]] = None
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
    lastUpdatedTime: Instant = Instant.ofEpochMilli(0),
    dataSetId: Option[Long] = None
) extends WithId[Long]
    with WithExternalId
    with WithCreatedTime

final case class SequenceCreate(
    name: Option[String] = None,
    description: Option[String] = None,
    assetId: Option[Long] = None,
    externalId: Option[String] = None,
    metadata: Option[Map[String, String]] = None,
    columns: NonEmptyList[SequenceColumnCreate],
    dataSetId: Option[Long] = None
) extends WithExternalId

final case class SequenceUpdate(
    name: Option[Setter[String]] = None,
    description: Option[Setter[String]] = None,
    assetId: Option[Setter[Long]] = None,
    externalId: Option[Setter[String]] = None,
    metadata: Option[NonNullableSetter[Map[String, String]]] = None,
    dataSetId: Option[Setter[Long]] = None
) extends WithSetExternalId

final case class SequenceFilter(
    name: Option[String] = None,
    externalIdPrefix: Option[String] = None,
    metadata: Option[Map[String, String]] = None,
    assetIds: Option[Seq[Long]] = None,
    rootAssetIds: Option[Seq[Long]] = None,
    createdTime: Option[TimeRange] = None,
    lastUpdatedTime: Option[TimeRange] = None,
    dataSetIds: Option[Seq[CogniteId]] = None
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
