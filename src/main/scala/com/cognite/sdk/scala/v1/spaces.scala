// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1

import java.time.Instant
import com.cognite.sdk.scala.common._

final case class Space(
    space: String,
    description: Option[String] = None,
    name: Option[String] = None,
    createdTime: Instant = Instant.ofEpochMilli(0),
    lastUpdatedTime: Instant = Instant.ofEpochMilli(0)
) extends WithCreatedTime
    with ToCreate[SpaceCreate] {
  override def toCreate: SpaceCreate =
    SpaceCreate(space, name, description)
}

final case class SpaceCreate(
    space: String,
    description: Option[String],
    name: Option[String]
)

final case class SpaceId(space: String)
