// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1

final case class SpacedItems[A](spaceExternalId: String, items: Seq[A])

final case class DataModelIdentifier(
    space: Option[String],
    model: String
)

final case class DataModelProperty(
    `type`: PropertyType,
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
    properties: Option[Map[String, DataModelProperty]] = None,
    `extends`: Option[Seq[DataModelIdentifier]] = None,
    indexes: Option[DataModelIndexes] = None,
    constraints: Option[DataModelConstraints] = None,
    instanceType: DataModelInstanceType = DataModelInstanceType.node
)

final case class DataModelInstanceType(allowNode: Boolean, allowEdge: Boolean)

object DataModelInstanceType {
  var node: DataModelInstanceType = DataModelInstanceType(true, false)
  var edge: DataModelInstanceType = DataModelInstanceType(false, true)
}

final case class DataModelListInput(spaceExternalId: String)
