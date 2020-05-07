package com.cognite.sdk.scala.v1

import java.time.Instant

import com.cognite.sdk.scala.common.{
  NonNullableSetter,
  SearchQuery,
  Setter,
  WithCreatedTime,
  WithExternalId,
  WithId,
  WithSetExternalId
}

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
    dataSetId: Option[Long] = None,
    sourceCreatedTime: Option[Instant] = None,
    sourceModifiedTime: Option[Instant] = None,
    securityCategories: Option[Seq[Long]] = None,
    uploadUrl: Option[String] = None
) extends WithId[Long]
    with WithExternalId
    with WithCreatedTime

final case class FileCreate(
    name: String,
    source: Option[String] = None,
    externalId: Option[String] = None,
    mimeType: Option[String] = None,
    metadata: Option[Map[String, String]] = None,
    assetIds: Option[Seq[Long]] = None,
    dataSetId: Option[Long] = None,
    sourceCreatedTime: Option[Instant] = None,
    sourceModifiedTime: Option[Instant] = None,
    securityCategories: Option[Seq[Long]] = None
) extends WithExternalId

final case class FileUpdate(
    externalId: Option[Setter[String]] = None,
    source: Option[Setter[String]] = None,
    mimeType: Option[Setter[String]] = None,
    metadata: Option[NonNullableSetter[Map[String, String]]] = None,
    assetIds: Option[NonNullableSetter[Seq[Long]]] = None,
    sourceCreatedTime: Option[Setter[Instant]] = None,
    sourceModifiedTime: Option[Setter[Instant]] = None,
    securityCategories: Option[NonNullableSetter[Seq[Long]]] = None,
    dataSetId: Option[Setter[Long]] = None
) extends WithSetExternalId

final case class FilesFilter(
    name: Option[String] = None,
    mimeType: Option[String] = None,
    metadata: Option[Map[String, String]] = None,
    assetIds: Option[Seq[Long]] = None,
    rootAssetIds: Option[Seq[CogniteId]] = None,
    source: Option[String] = None,
    createdTime: Option[TimeRange] = None,
    lastUpdatedTime: Option[TimeRange] = None,
    uploadedTime: Option[TimeRange] = None,
    sourceCreatedTime: Option[TimeRange] = None,
    sourceModifiedTime: Option[TimeRange] = None,
    externalIdPrefix: Option[String] = None,
    uploaded: Option[Boolean] = None,
    dataSetIds: Option[Seq[CogniteId]] = None
)

final case class FilesSearch(
    name: Option[String] = None
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
