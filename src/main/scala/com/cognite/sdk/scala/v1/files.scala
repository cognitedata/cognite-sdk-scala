package com.cognite.sdk.scala.v1

import java.time.Instant

import com.cognite.sdk.scala.common.{NonNullableSetter, SearchQuery, Setter, WithExternalId, WithId}

final case class File(
    id: Long = 0,
    name: String,
    source: Option[String] = None,
    externalId: Option[String] = None,
    mimeType: Option[String] = None,
    metadata: Option[Map[String, String]] = None,
    assetIds: Option[Seq[Long]] = None,
    uploaded: Boolean = false,
    uploadedTime: Option[Instant] = None,
    createdTime: Instant = Instant.ofEpochMilli(0),
    lastUpdatedTime: Instant = Instant.ofEpochMilli(0),
    uploadUrl: Option[String] = None
) extends WithId[Long]
    with WithExternalId

final case class FileCreate(
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
    name: Option[String] = None,
    mimeType: Option[String] = None,
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

sealed trait FileDownload
final case class FileDownloadId(id: Long) extends FileDownload
final case class FileDownloadExternalId(externalId: String) extends FileDownload

sealed trait FileDownloadLink {
  def downloadUrl: String
}
final case class FileDownloadLinkId(
    id: Long,
    downloadUrl: String
) extends FileDownloadLink

final case class FileDownloadLinkExternalId(
    externalId: String,
    downloadUrl: String
) extends FileDownloadLink
