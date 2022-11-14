package com.cognite.sdk.scala.v1.containers

import java.time.Instant

final case class ContainerReference(space: String, externalId: String) {
  val `type` = "container"
}

final case class IndexPropertyReference(
    container: ContainerReference,
    identifier: Option[String]
)

final case class IndexDefinition(properties: Seq[IndexPropertyReference]) {
  val indexType = "btree"
}

final case class ConstraintProperty(container: ContainerReference, identifier: Option[String])

final case class ConstraintDefinition(
    constraintType: ConstraintType,
    properties: Seq[ConstraintProperty]
)

final case class ContainerPropertyDefinition(
    nullable: Option[Boolean] = Some(true),
    autoIncrement: Option[Boolean] = Some(false),
    defaultValue: Option[PropertyDefaultValue],
    description: Option[String],
    name: Option[String],
    `type`: ContainerPropertyType
)

final case class Container(
    space: String,
    externalId: String,
    name: Option[String],
    description: Option[String],
    usedFor: Option[ContainerUsage],
    properties: Map[String, ContainerPropertyDefinition],
    constraints: Option[Map[String, ConstraintDefinition]],
    indexes: Option[Map[String, IndexDefinition]]
)

final case class ContainerRead(
    space: String,
    externalId: String,
    name: Option[String],
    description: Option[String],
    usedFor: ContainerUsage,
    properties: Map[String, ContainerPropertyDefinition],
    constraints: Option[Map[String, ConstraintDefinition]],
    indexes: Option[Map[String, IndexDefinition]],
    createdTime: Instant,
    lastUpdatedTime: Instant
)
