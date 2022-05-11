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

final case class DataModelProperty(
    `type`: PropertyType.Value,
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
    properties: Option[Map[String, DataModelProperty]] = None,
    `extends`: Option[Seq[DataModelIdentifier]] = None,
    indexes: Option[DataModelIndexes] = None,
    constraints: Option[DataModelConstraints] = None,
    instanceType: DataModelInstanceType = DataModelInstanceType.Node
) {
  private[v1] def toDTO: DataModelDTO =
    DataModelDTO(
      externalId,
      properties,
      `extends`,
      indexes,
      constraints,
      allowEdge = instanceType == DataModelInstanceType.Edge,
      allowNode = instanceType == DataModelInstanceType.Node
    )
}

object DataModel {
  private[v1] def fromDTO(dto: DataModelDTO): DataModel = {
    val instanceType =
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
    properties: Option[Map[String, DataModelProperty]] = None,
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

final case class DataModelInstanceCreate(
    modelExternalId: String,
    properties: Option[Map[String, PropertyType]] = None
)

sealed trait DataModelInstanceFilter

sealed trait DMIBoolFilter extends DataModelInstanceFilter
final case class DMIAndFilter(and: Seq[DataModelInstanceFilter]) extends DMIBoolFilter
final case class DMIOrFilter(or: Seq[DataModelInstanceFilter]) extends DMIBoolFilter
final case class DMINotFilter(not: DataModelInstanceFilter) extends DMIBoolFilter

sealed trait DMILeafFilter extends DataModelInstanceFilter
final case class DMIEqualsFilter(property: Seq[String], value: PropertyType) extends DMILeafFilter
final case class DMIInFilter(property: Seq[String], values: Seq[PropertyType]) extends DMILeafFilter
final case class DMIRangeFilter(
    property: Seq[String],
    gte: Option[PropertyType] = None,
    gt: Option[PropertyType] = None,
    lte: Option[PropertyType] = None,
    lt: Option[PropertyType] = None
) extends DMILeafFilter {
  require(
    !(gte.isDefined && gt.isDefined) && // can't have both upper bound in the same time
      !(lte.isDefined && lt.isDefined) && // can't have both lower bound in the same time
      (gte.isDefined || gt.isDefined || lte.isDefined || lt.isDefined) // at least one bound must be defined
  )
}
final case class DMIPrefixFilter(property: Seq[String], value: PropertyType) extends DMILeafFilter
final case class DMIExistsFilter(property: Seq[String]) extends DMILeafFilter
final case class DMIContainsAnyFilter(property: Seq[String], values: Seq[PropertyType])
    extends DMILeafFilter
final case class DMIContainsAllFilter(property: Seq[String], values: Seq[PropertyType])
    extends DMILeafFilter

final case class DataModelInstanceQuery(
    modelExternalId: String,
    filter: Option[DataModelInstanceFilter] = None,
    sort: Option[Seq[String]] = None,
    limit: Option[Int] = None,
    cursor: Option[String] = None
)

final case class DataModelInstanceQueryResponse(
    modelExternalId: String,
    properties: Option[Map[String, PropertyType]] = None
)

final case class DataModelInstanceByExternalId(
    externalId: String,
    modelExternalId: String
)
