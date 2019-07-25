package com.cognite.sdk.scala.v1

import java.time.Instant

import com.cognite.sdk.scala.common.{NonNullableSetter, WithId}

final case class ThreeDModel(
    name: String,
    id: Long = 0,
    createdTime: Instant = Instant.ofEpochMilli(0),
    metadata: Option[Map[String, String]] = None
) extends WithId[Long]

final case class CreateThreeDModel(
    name: String,
    metadata: Option[Map[String, String]] = None
)

final case class ThreeDModelUpdate(
    id: Long = 0,
    name: Option[NonNullableSetter[String]] = None,
    metadata: Option[NonNullableSetter[Map[String, String]]] = None
) extends WithId[Long]

final case class Camera(
    target: Option[Array[Double]],
    position: Option[Array[Double]]
)

final case class ThreeDRevision(
    id: Long = 0,
    fileId: Long,
    published: Boolean = false,
    rotation: Option[Array[Double]] = None,
    camera: Option[Camera] = None,
    status: String = "",
    metadata: Option[Map[String, String]] = None,
    thumbnailThreedFileId: Option[Long] = None,
    thumbnailURL: Option[String] = None,
    assetMappingCount: Long = 0,
    createdTime: Instant = Instant.ofEpochMilli(0)
) extends WithId[Long]

final case class CreateThreeDRevision(
    published: Boolean,
    rotation: Option[Array[Double]] = None,
    metadata: Option[Map[String, String]] = None,
    camera: Option[Camera] = None,
    fileId: Long
)

final case class ThreeDRevisionUpdate(
    id: Long = 0,
    published: Option[NonNullableSetter[Boolean]],
    rotation: Option[NonNullableSetter[Array[Double]]] = None,
    camera: Option[NonNullableSetter[Camera]] = None,
    metadata: Option[NonNullableSetter[Map[String, String]]] = None
) extends WithId[Long]

final case class ThreeDAssetMapping(
    nodeId: Long,
    assetId: Long,
    treeIndex: Long = 0,
    subtreeSize: Long = 0
)

final case class CreateThreeDAssetMapping(
    nodeId: Long,
    assetId: Long
)
