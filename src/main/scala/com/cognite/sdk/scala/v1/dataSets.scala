package com.cognite.sdk.scala.v1

import java.time.Instant

import com.cognite.sdk.scala.common._

private[sdk] final case class DataSetCreate(
    name: Option[String],
    description: Option[String] = None,
    externalId: Option[String] = None,
    metadata: Map[String, String] = Map(),
    writeProtected: Boolean = false
) extends WithExternalId

private[sdk] final case class DataSetFilter(
    externalIdPrefix: Option[String] = None,
    metadata: Option[Map[String, String]] = None,
    writeProtected: Option[Boolean] = None,
    createdTime: Option[TimeRange] = None,
    lastUpdatedTime: Option[TimeRange] = None
)

private[sdk] final case class DataSetUpdate(
    externalId: Option[Setter[String]] = None,
    name: Option[Setter[String]] = None,
    description: Option[Setter[String]] = None,
    metadata: Option[NonNullableSetter[Map[String, String]]] = None,
    writeProtected: Option[NonNullableSetter[Boolean]] = None
) extends WithSetExternalId

private[sdk] final case class DataSet(
    name: Option[String],
    writeProtected: Boolean,
    externalId: Option[String],
    description: Option[String],
    metadata: Map[String, String],
    id: Long,
    createdTime: Instant,
    lastUpdatedTime: Instant
) extends WithId[Long]
    with WithExternalId

private[sdk] final case class DataSetQuery(
    filter: Option[DataSetFilter],
    limit: Int
)
