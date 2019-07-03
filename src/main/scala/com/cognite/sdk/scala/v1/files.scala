package com.cognite.sdk.scala.v1

import com.cognite.sdk.scala.common.WithId

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
