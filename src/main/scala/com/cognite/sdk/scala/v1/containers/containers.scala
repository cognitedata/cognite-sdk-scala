// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1.containers

import java.time.Instant

final case class ContainerPropertyDefinition(
    nullable: Option[Boolean] = Some(true),
    autoIncrement: Option[Boolean] = Some(false),
    defaultValue: Option[PropertyDefaultValue],
    description: Option[String],
    name: Option[String],
    `type`: ContainerPropertyType
)

final case class ContainerCreate(
    space: String,
    externalId: String,
    name: Option[String],
    description: Option[String],
    usedFor: Option[ContainerUsage],
    properties: Map[String, ContainerPropertyDefinition],
    constraints: Option[Map[String, ContainerConstraint]],
    indexes: Option[Map[String, IndexDefinition]]
)

final case class ContainerRead(
    space: String,
    externalId: String,
    name: Option[String],
    description: Option[String],
    usedFor: ContainerUsage,
    properties: Map[String, ContainerPropertyDefinition],
    constraints: Option[Map[String, ContainerConstraint]],
    indexes: Option[Map[String, IndexDefinition]],
    createdTime: Instant,
    lastUpdatedTime: Instant
)

final case class ContainerId(space: String, externalId: String)
