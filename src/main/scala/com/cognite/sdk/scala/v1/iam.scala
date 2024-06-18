// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1

import java.time.Instant
import com.cognite.sdk.scala.common.{ToCreate, WithId}
import io.circe.Json

final case class ProjectAuthentication(
    validDomains: Seq[String]
)

final case class Project(
    name: String,
    urlName: String,
    defaultGroupId: Option[Long],
    authentication: Option[ProjectAuthentication]
)

final case class Capability(
    actions: Seq[String],
    scope: Map[String, Map[String, Json]]
)

final case class Group(
    name: String,
    sourceId: Option[String],
    capabilities: Seq[Map[String, Capability]],
    id: Long,
    isDeleted: Boolean,
    deletedTime: Option[Instant]
) extends WithId[Long]
    with ToCreate[GroupCreate] {
  override def toCreate: GroupCreate =
    GroupCreate(
      name = name,
      sourceId = sourceId,
      capabilities = capabilities
    )
}

final case class GroupCreate(
    name: String,
    sourceId: Option[String],
    capabilities: Seq[Map[String, Capability]]
) {}

final case class SecurityCategory(
    name: String,
    id: Long
) extends WithId[Long]
