// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1

import com.cognite.sdk.scala.v1.PropertyType.AnyPropertyType

final case class SpacedItems[A](spaceExternalId: String, items: Seq[A])

final case class DataModelIdentifier(
    space: Option[String],
    model: String
)

final case class DataModelPropertyDeffinition(
    `type`: AnyPropertyType,
    nullable: Boolean = true,
    targetModel: Option[DataModelIdentifier] = None
)

final case class UniquenessConstraint(
    uniqueProperties: Seq[String]
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
    dataModelType: DataModelType = DataModelType.NodeType
)

sealed abstract class DataModelType

object DataModelType {
  case object NodeType extends DataModelType
  case object EdgeType extends DataModelType
}

final case class DataModelListInput(spaceExternalId: String)
