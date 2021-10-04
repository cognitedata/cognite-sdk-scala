// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1

import java.time.Instant

import com.cognite.sdk.scala.common.{
  NonNullableIterableSetter,
  NonNullableSetter,
  ToCreate,
  ToUpdate,
  WithId
}

final case class ThreeDModel(
    name: String,
    id: Long = 0,
    createdTime: Instant = Instant.ofEpochMilli(0),
    metadata: Option[Map[String, String]] = None
) extends WithId[Long]
    with ToCreate[ThreeDModelCreate]
    with ToUpdate[ThreeDModelUpdate] {
  override def toCreate: ThreeDModelCreate = ThreeDModelCreate(name, metadata)

  override def toUpdate: ThreeDModelUpdate =
    ThreeDModelUpdate(
      id,
      Some(NonNullableSetter.fromAny(name)),
      NonNullableIterableSetter.fromOption(metadata)
    )
}

final case class ThreeDModelCreate(
    name: String,
    metadata: Option[Map[String, String]] = None
)

final case class ThreeDModelUpdate(
    id: Long = 0,
    name: Option[NonNullableSetter[String]] = None,
    metadata: Option[NonNullableIterableSetter[Map[String, String]]] = None
) extends WithId[Long]

final case class Camera(
    target: Option[Seq[Double]],
    position: Option[Seq[Double]]
)

final case class BoundingBox(
    max: Seq[Double],
    min: Seq[Double]
)

final case class Properties(
    properties: Map[String, Map[String, String]]
)

final case class ThreeDRevision(
    id: Long = 0,
    fileId: Long,
    published: Boolean = false,
    rotation: Option[Seq[Double]] = None,
    camera: Option[Camera] = None,
    status: String = "",
    metadata: Option[Map[String, String]] = None,
    thumbnailThreedFileId: Option[Long] = None,
    thumbnailURL: Option[String] = None,
    assetMappingCount: Long = 0,
    createdTime: Instant = Instant.ofEpochMilli(0)
) extends WithId[Long]
    with ToCreate[ThreeDRevisionCreate]
    with ToUpdate[ThreeDRevisionUpdate] {
  override def toCreate: ThreeDRevisionCreate =
    ThreeDRevisionCreate(
      published,
      rotation,
      metadata,
      camera,
      fileId
    )

  override def toUpdate: ThreeDRevisionUpdate =
    ThreeDRevisionUpdate(
      id,
      Some(NonNullableSetter.fromAny(published)),
      NonNullableSetter.fromOption(rotation),
      NonNullableSetter.fromOption(camera),
      NonNullableIterableSetter.fromOption(metadata)
    )
}

final case class ThreeDRevisionCreate(
    published: Boolean,
    rotation: Option[Seq[Double]] = None,
    metadata: Option[Map[String, String]] = None,
    camera: Option[Camera] = None,
    fileId: Long
)

final case class ThreeDRevisionUpdate(
    id: Long = 0,
    published: Option[NonNullableSetter[Boolean]],
    rotation: Option[NonNullableSetter[Seq[Double]]] = None,
    camera: Option[NonNullableSetter[Camera]] = None,
    metadata: Option[NonNullableIterableSetter[Map[String, String]]] = None
) extends WithId[Long]

final case class ThreeDAssetMapping(
    nodeId: Long,
    assetId: Long,
    treeIndex: Option[Long],
    subtreeSize: Option[Long]
)

final case class ThreeDAssetMappingCreate(
    nodeId: Long,
    assetId: Long
)

final case class ThreeDNode(
    id: Long,
    treeIndex: Long,
    parentId: Option[Long],
    depth: Long,
    name: String,
    subtreeSize: Long,
    properties: Option[Map[String, Map[String, String]]],
    boundingBox: Option[BoundingBox]
)

final case class ThreeDNodeFilter(
    limit: Int,
    depth: Int,
    nodeId: Long,
    properties: Properties
)
