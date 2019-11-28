package com.cognite.sdk.scala.v1

import java.time.Instant

import com.cognite.sdk.scala.common.WithId
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

final case class ServiceAccount(
    name: String,
    groups: Seq[Long],
    id: Long,
    isDeleted: Boolean,
    deletedTime: Option[Instant]
) extends WithId[Long]

final case class ApiKey(
    id: Long,
    serviceAccountId: Long,
    createdTime: Instant,
    status: String
) extends WithId[Long]

final case class Capability(
    version: Int,
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

final case class SecurityCategory(
    name: String,
    id: Long
) extends WithId[Long]
