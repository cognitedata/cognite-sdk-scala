package com.cognite.sdk.scala.v1

import java.time.Instant

import com.cognite.sdk.scala.common.WithCreatedTime

final case class Label(
    externalId: String,
    name: String,
    description: Option[String] = None,
    createdTime: Instant = Instant.ofEpochMilli(0)
) extends WithCreatedTime

final case class LabelCreate(
    externalId: String,
    name: String,
    description: Option[String] = None
)

final case class LabelFilter(
    name: Option[String] = None,
    externalIdPrefix: Option[String] = None
)

final case class LabelQuery(
    filter: Option[LabelFilter],
    limit: Int
)
