package com.cognite.sdk.scala.v1

import java.time.Instant

import com.cognite.sdk.scala.common._

final case class DataSet(
    id: Long = 0,
    name: Option[String] = None,
    writeProtected: Boolean = false,
    externalId: Option[String] = None,
    description: Option[String] = None,
    metadata: Option[Map[String, String]] = None,
    createdTime: Instant = Instant.ofEpochMilli(0),
    lastUpdatedTime: Instant = Instant.ofEpochMilli(0)
) extends WithId[Long]
    with WithExternalId
    with WithCreatedTime

final case class DataSetCreate(
    externalId: Option[String] = None,
    name: Option[String],
    description: Option[String] = None,
    metadata: Option[Map[String, String]] = None,
    writeProtected: Boolean = false
) extends WithExternalId

final case class DataSetUpdate(
    externalId: Option[Setter[String]] = None,
    name: Option[Setter[String]] = None,
    description: Option[Setter[String]] = None,
    metadata: Option[NonNullableSetter[Map[String, String]]] = None,
    writeProtected: Option[NonNullableSetter[Boolean]] = None
) extends WithSetExternalId

final case class DataSetFilter(
    metadata: Option[Map[String, String]] = None,
    createdTime: Option[TimeRange] = None,
    lastUpdatedTime: Option[TimeRange] = None,
    externalIdPrefix: Option[String] = None,
    writeProtected: Option[Boolean] = None
)

final case class DataSetQuery(
    filter: Option[DataSetFilter],
    limit: Int
)
