package com.cognite.sdk.scala.v1

import com.cognite.sdk.scala.common.{NonNullableSetter, Setter, WithId}

final case class Asset(
    id: Long = 0,
    path: Option[Seq[Long]] = None,
    depth: Option[Long] = None,
    name: String,
    parentId: Option[Long] = None,
    description: Option[String] = None,
    metadata: Option[Map[String, String]] = None,
    source: Option[String] = None,
    externalId: Option[String] = None,
    createdTime: Option[Long] = None,
    lastUpdatedTime: Option[Long] = None
) extends WithId[Long]

final case class CreateAsset(
    name: String,
    parentId: Option[Long] = None,
    description: Option[String] = None,
    source: Option[String] = None,
    externalId: Option[String] = None,
    metadata: Option[Map[String, String]] = None
)

final case class AssetUpdate(
    id: Long,
    name: Option[NonNullableSetter[String]] = None,
    description: Option[Setter[String]] = None,
    source: Option[Setter[String]] = None,
    externalId: Option[Setter[String]] = None,
    metadata: Option[NonNullableSetter[Map[String, String]]] = None
) extends WithId[Long]
