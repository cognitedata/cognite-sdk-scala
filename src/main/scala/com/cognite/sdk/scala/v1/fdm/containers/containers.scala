// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1.fdm.containers

import com.cognite.sdk.scala.v1.fdm.common.Usage
import com.cognite.sdk.scala.v1.fdm.common.properties.PropertyDefinition.ContainerPropertyDefinition

import java.time.Instant

final case class ContainerCreateDefinition(
    space: String,
    externalId: String,
    name: Option[String],
    description: Option[String],
    usedFor: Option[Usage],
    properties: Map[String, ContainerPropertyDefinition],
    constraints: Option[Map[String, ContainerConstraint]],
    indexes: Option[Map[String, IndexDefinition]]
)

final case class ContainerDefinition(
    space: String,
    externalId: String,
    name: Option[String],
    description: Option[String],
    usedFor: Usage,
    properties: Map[String, ContainerPropertyDefinition],
    constraints: Option[Map[String, ContainerConstraint]],
    indexes: Option[Map[String, IndexDefinition]],
    createdTime: Instant,
    lastUpdatedTime: Instant
) {
  def toContainerReference: ContainerReference = ContainerReference(space, externalId)
}

final case class ContainerId(space: String, externalId: String)
