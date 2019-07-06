package com.cognite.sdk.scala.v1

import com.cognite.sdk.scala.common.{NonNullableSetter, Setter, SearchQuery, TimeRange, WithId}

final case class File(
    id: Long = 0,
    name: String,
    source: Option[String] = None,
    externalId: Option[String] = None,
    mimeType: Option[String] = None,
    metadata: Option[Map[String, String]] = None,
    assetIds: Option[Seq[Long]] = None,
    uploaded: Boolean = false,
    uploadedTime: Option[Long] = None,
    createdTime: Long = 0,
    lastUpdatedTime: Long = 0,
    uploadUrl: Option[String] = None
) extends WithId[Long]

final case class CreateFile(
    name: String,
    source: Option[String] = None,
    externalId: Option[String] = None,
    mimeType: Option[String] = None,
    metadata: Option[Map[String, String]] = None,
    assetIds: Option[Seq[Long]] = None
)

final case class FileUpdate(
    id: Long = 0,
    externalId: Option[Setter[String]] = None,
    source: Option[Setter[String]] = None,
    metadata: Option[NonNullableSetter[Map[String, String]]] = None,
    assetIds: Option[NonNullableSetter[Seq[Long]]] = None
) extends WithId[Long]

final case class FilesFilter(
    name: Option[String],
    mimeType: Option[String],
    metadata: Option[Map[String, String]] = None,
    assetIds: Option[Seq[Long]] = None,
    source: Option[String] = None,
    createdTime: Option[TimeRange] = None,
    lastUpdatedTime: Option[TimeRange] = None,
    uploadedTime: Option[TimeRange] = None,
    externalIdPrefix: Option[String] = None,
    uploaded: Option[Boolean] = None
)

final case class FilesSearch(
    name: Option[String]
)

final case class FilesQuery(
    filter: Option[FilesFilter] = None,
    search: Option[FilesSearch] = None,
    limit: Int = 100
) extends SearchQuery[FilesFilter, FilesSearch]
