package com.cognite.sdk.scala.v1

final case class DataModelInstanceCreate(
    modelExternalId: String,
    properties: Option[Map[String, DataModelProperty]] = None
)

sealed trait DataModelInstanceFilter

sealed trait DMIBoolFilter extends DataModelInstanceFilter
final case class DMIAndFilter(and: Seq[DataModelInstanceFilter]) extends DMIBoolFilter
final case class DMIOrFilter(or: Seq[DataModelInstanceFilter]) extends DMIBoolFilter
final case class DMINotFilter(not: DataModelInstanceFilter) extends DMIBoolFilter

sealed trait DMILeafFilter extends DataModelInstanceFilter
final case class DMIEqualsFilter(property: Seq[String], value: DataModelProperty) extends DMILeafFilter
final case class DMIInFilter(property: Seq[String], values: Seq[DataModelProperty]) extends DMILeafFilter
final case class DMIRangeFilter(
    property: Seq[String],
    gte: Option[DataModelProperty] = None,
    gt: Option[DataModelProperty] = None,
    lte: Option[DataModelProperty] = None,
    lt: Option[DataModelProperty] = None
) extends DMILeafFilter {
  require(
    !(gte.isDefined && gt.isDefined) && // can't have both upper bound in the same time
      !(lte.isDefined && lt.isDefined) && // can't have both lower bound in the same time
      (gte.isDefined || gt.isDefined || lte.isDefined || lt.isDefined) // at least one bound must be defined
  )
}
final case class DMIPrefixFilter(property: Seq[String], value: DataModelProperty) extends DMILeafFilter
final case class DMIExistsFilter(property: Seq[String]) extends DMILeafFilter
final case class DMIContainsAnyFilter(property: Seq[String], values: Seq[DataModelProperty])
    extends DMILeafFilter
final case class DMIContainsAllFilter(property: Seq[String], values: Seq[DataModelProperty])
    extends DMILeafFilter

final case class DataModelInstanceQuery(
    model: DataModelIdentifier,
    filter: Option[DataModelInstanceFilter] = None,
    sort: Option[Seq[String]] = None,
    limit: Option[Int] = None,
    cursor: Option[String] = None
)

final case class DataModelInstanceQueryResponse(
    modelExternalId: String,
    properties: Option[Map[String, DataModelProperty]] = None
)

final case class DataModelInstanceByExternalId(
    externalId: String,
    modelExternalId: String
)
