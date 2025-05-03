// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1

import java.time.Instant
import com.cognite.sdk.scala.common.{
  NonNullableSetter,
  SearchQuery,
  Setter,
  ToCreate,
  ToUpdate,
  WithCreatedTime,
  WithExternalId,
  WithId,
  WithSetExternalId
}

final case class File(
    id: Long = 0,
    name: String,
    directory: Option[String] = None,
    source: Option[String] = None,
    externalId: Option[String] = None,
    instanceId: Option[InstanceId] = None,
    mimeType: Option[String] = None,
    metadata: Option[Map[String, String]] = None,
    assetIds: Option[Seq[Long]] = None,
    uploaded: Boolean = false,
    uploadedTime: Option[Instant] = None,
    createdTime: Instant = Instant.ofEpochMilli(0),
    lastUpdatedTime: Instant = Instant.ofEpochMilli(0),
    dataSetId: Option[Long] = None,
    labels: Option[Seq[CogniteExternalId]] = None,
    sourceCreatedTime: Option[Instant] = None,
    sourceModifiedTime: Option[Instant] = None,
    securityCategories: Option[Seq[Long]] = None,
    uploadUrl: Option[String] = None
    // TODO: geoLocation object
) extends WithId[Long]
    with WithExternalId
    with WithCreatedTime
    with ToCreate[FileCreate]
    with ToUpdate[FileUpdate] {
  override def toCreate: FileCreate =
    FileCreate(
      name,
      directory,
      source,
      externalId,
      mimeType,
      metadata,
      assetIds,
      dataSetId,
      sourceCreatedTime,
      sourceModifiedTime,
      securityCategories,
      labels
    )

  override def toUpdate: FileUpdate =
    FileUpdate(
      Setter.fromOption(externalId),
      Setter.fromOption(source),
      Setter.fromOption(directory),
      Setter.fromOption(mimeType),
      NonNullableSetter.fromOption(metadata),
      NonNullableSetter.fromOption(assetIds),
      Setter.fromOption(sourceCreatedTime),
      Setter.fromOption(sourceModifiedTime),
      NonNullableSetter.fromOption(securityCategories),
      Setter.fromOption(dataSetId),
      NonNullableSetter.fromOption(labels)
    )
}

final case class FileCreate(
    name: String,
    directory: Option[String] = None,
    source: Option[String] = None,
    externalId: Option[String] = None,
    mimeType: Option[String] = None,
    metadata: Option[Map[String, String]] = None,
    assetIds: Option[Seq[Long]] = None,
    dataSetId: Option[Long] = None,
    sourceCreatedTime: Option[Instant] = None,
    sourceModifiedTime: Option[Instant] = None,
    securityCategories: Option[Seq[Long]] = None,
    labels: Option[Seq[CogniteExternalId]] = None
) extends WithExternalId

final case class FileUpdate(
    externalId: Option[Setter[String]] = None,
    source: Option[Setter[String]] = None,
    directory: Option[Setter[String]] = None,
    mimeType: Option[Setter[String]] = None,
    metadata: Option[NonNullableSetter[Map[String, String]]] = None,
    assetIds: Option[NonNullableSetter[Seq[Long]]] = None,
    sourceCreatedTime: Option[Setter[Instant]] = None,
    sourceModifiedTime: Option[Setter[Instant]] = None,
    securityCategories: Option[NonNullableSetter[Seq[Long]]] = None,
    dataSetId: Option[Setter[Long]] = None,
    labels: Option[NonNullableSetter[Seq[CogniteExternalId]]] = None
) extends WithSetExternalId

final case class FilesFilter(
    name: Option[String] = None,
    directoryPrefix: Option[String] = None,
    mimeType: Option[String] = None,
    metadata: Option[Map[String, String]] = None,
    assetIds: Option[Seq[Long]] = None,
    assetExternalIds: Option[Seq[String]] = None,
    rootAssetIds: Option[Seq[CogniteId]] = None,
    assetSubtreeIds: Option[Seq[CogniteId]] = None,
    source: Option[String] = None,
    createdTime: Option[TimeRange] = None,
    lastUpdatedTime: Option[TimeRange] = None,
    uploadedTime: Option[TimeRange] = None,
    sourceCreatedTime: Option[TimeRange] = None,
    sourceModifiedTime: Option[TimeRange] = None,
    externalIdPrefix: Option[String] = None,
    uploaded: Option[Boolean] = None,
    dataSetIds: Option[Seq[CogniteId]] = None,
    labels: Option[LabelContainsFilter] = None
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
final case class FileDownloadInstanceId(instanceId: InstanceId) extends FileDownload

sealed trait FileUpload
final case class FileUploadExternalId(externalId: String) extends FileUpload
final case class FileUploadInstanceId(instanceId: InstanceId) extends FileUpload

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

final case class FileDownloadLinkInstanceId(
    instanceId: InstanceId,
    downloadUrl: String
) extends FileDownloadLink
