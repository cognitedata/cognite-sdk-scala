// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1

final case class SpacedItems[A](spaceExternalId: String, items: Seq[A])

final case class DataModelIdentifier(
    space: Option[String],
    model: String
)

object DataModelIdentifier {
  def apply(model: String): DataModelIdentifier =
    DataModelIdentifier(None, model)
  def apply(space: String, model: String): DataModelIdentifier =
    DataModelIdentifier(Some(space), model)
}

final case class DataModelPropertyDeffinition(
    `type`: PropertyType,
    nullable: Boolean = true,
    targetModel: Option[DataModelIdentifier] = None
)

final case class ContstrainedProperty(property: String)

final case class UniquenessConstraint(
    uniqueProperties: Seq[ContstrainedProperty]
)

final case class DataModelConstraints(
    uniqueness: Option[Map[String, UniquenessConstraint]] = None
)

final case class BTreeIndex(
    properties: Seq[String]
)

final case class DataModelIndexes(
    btreeIndex: Option[Map[String, BTreeIndex]] = None
)

final case class DataModel(
    externalId: String,
    properties: Option[Map[String, DataModelPropertyDeffinition]] = None,
    `extends`: Option[Seq[DataModelIdentifier]] = None,
    indexes: Option[DataModelIndexes] = None,
    constraints: Option[DataModelConstraints] = None,
    instanceType: DataModelInstanceType = DataModelInstanceType.Node
) {
  private val (_allowEdge, _allowNode) = instanceType match {
    case DataModelInstanceType.Edge => (true, false)
    case DataModelInstanceType.Node => (false, true)
  }
  private[v1] def toDTO: DataModelDTO =
    DataModelDTO(
      externalId,
      properties,
      `extends`,
      indexes,
      constraints,
      _allowEdge,
      _allowNode
    )
}

object DataModel {
  private[v1] def fromDTO(dto: DataModelDTO): DataModel = {
    val instanceType: DataModelInstanceType =
      (dto.allowEdge, dto.allowNode) match {
        case (true, false) => DataModelInstanceType.Edge
        case (false, true) => DataModelInstanceType.Node
        case _ =>
          throw new IllegalArgumentException("Exactly one of allowNode and allowEdge must be true")
      }

    DataModel(
      dto.externalId,
      dto.properties,
      dto.`extends`,
      dto.indexes,
      dto.constraints,
      instanceType
    )
  }
}

private final case class DataModelDTO(
    externalId: String,
    properties: Option[Map[String, DataModelPropertyDeffinition]] = None,
    `extends`: Option[Seq[DataModelIdentifier]] = None,
    indexes: Option[DataModelIndexes] = None,
    constraints: Option[DataModelConstraints] = None,
    allowEdge: Boolean = false,
    allowNode: Boolean = true
)

sealed abstract class DataModelInstanceType

object DataModelInstanceType {
  case object Node extends DataModelInstanceType
  case object Edge extends DataModelInstanceType
}

final case class DataModelListInput(spaceExternalId: String)
