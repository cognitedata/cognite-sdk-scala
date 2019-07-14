package com.cognite.sdk.scala.v1

import com.cognite.sdk.scala.common.{NonNullableSetter, WithId}

final case class ThreeDModel(
    name: String,
    id: Long = 0,
    createdTime: Option[Long] = None,
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
)

final case class Camera(
    target: Option[Array[Double]],
    position: Option[Array[Double]]
)

final case class ThreeDRevision(
    id: Long,
    fileId: Long,
    published: Boolean,
    rotation: Option[Array[Double]] = None,
    camera: Option[Camera] = None,
    status: String,
    metadata: Option[Map[String, String]] = None,
    thumbnailThreedFileId: Option[Long] = None,
    thumbnailURL: Option[String] = None,
    assetMappingCount: Long,
    createdTime: Long
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
)
