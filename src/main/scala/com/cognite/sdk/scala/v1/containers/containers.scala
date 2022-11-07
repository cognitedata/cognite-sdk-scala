package com.cognite.sdk.scala.v1.containers

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
    nullable: Boolean = true,
    autoIncrement: Boolean = false,
    defaultValue: Option[PropertyDefaultValue],
    description: Option[String],
    name: Option[String],
    `type`: Option[ContainerPropertyType]
)

final case class ContainerCreate(
    space: String,
    externalId: String,
    name: Option[String],
    description: Option[String],
    usedFor: Option[ContainerUsage],
    properties: Map[String, ContainerPropertyDefinition],
    constraints: Option[Map[String, ConstraintDefinition]],
    indexes: Option[Map[String, IndexDefinition]]
)
