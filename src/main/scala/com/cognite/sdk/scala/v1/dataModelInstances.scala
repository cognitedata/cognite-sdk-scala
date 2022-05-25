package com.cognite.sdk.scala.v1

import com.cognite.sdk.scala.v1.DataModelProperties._

sealed class DataModelInstance(val properties: Map[String, AnyProperty]) {
  val externalId: String = properties.get("externalId") match {
    case Some(PropertyType.Text.Property(externalId)) => externalId
    case Some(invalidType) =>
      throw new Exception(
        s"externalId should be a TextProperty, it is ${invalidType.toString} instead"
      )
    case None =>
      throw new Exception("externalId is required")
  }
}

object Node {
  def Apply(
      externalId: String,
      `type`: Option[String] = None,
      name: Option[String] = None,
      description: Option[String] = None,
      properties: Option[Map[String, AnyProperty]] = None
  ): DataModelInstance = {

    val propsToAdd: Seq[Option[(String, AnyProperty)]] =
      Seq[(String, Option[DataModelProperty[_]])](
        "externalId" -> Some(PropertyType.Text.Property(externalId)),
        "type" -> `type`.map(PropertyType.Text.Property(_)),
        "name" -> name.map(PropertyType.Text.Property(_)),
        "description" -> description.map(PropertyType.Text.Property(_))
      ).map { case (k, v) =>
        v.map(k -> _)
      }

    val allProps = properties.getOrElse(Map.empty[String, AnyProperty]) ++
      propsToAdd.flatten.toMap

    new DataModelInstance(allProps)
  }
}

final case class DataModelInstanceCreate(
    spaceExternalId: String,
    model: DataModelIdentifier,
    overwrite: Boolean = false,
    items: Seq[DataModelInstance]
)

sealed trait DataModelInstanceFilter

sealed trait DMIBoolFilter extends DataModelInstanceFilter
final case class DMIAndFilter(and: Seq[DataModelInstanceFilter]) extends DMIBoolFilter
final case class DMIOrFilter(or: Seq[DataModelInstanceFilter]) extends DMIBoolFilter
final case class DMINotFilter(not: DataModelInstanceFilter) extends DMIBoolFilter

sealed trait DMILeafFilter extends DataModelInstanceFilter
final case class DMIEqualsFilter(property: Seq[String], value: AnyProperty) extends DMILeafFilter
final case class DMIInFilter(property: Seq[String], values: Seq[AnyProperty]) extends DMILeafFilter
final case class DMIRangeFilter(
    property: Seq[String],
    gte: Option[AnyProperty] = None,
    gt: Option[AnyProperty] = None,
    lte: Option[AnyProperty] = None,
    lt: Option[AnyProperty] = None
) extends DMILeafFilter {
  require(
    !(gte.isDefined && gt.isDefined) && // can't have both upper bound in the same time
      !(lte.isDefined && lt.isDefined) && // can't have both lower bound in the same time
      (gte.isDefined || gt.isDefined || lte.isDefined || lt.isDefined) // at least one bound must be defined
  )
}
final case class DMIPrefixFilter(property: Seq[String], value: AnyProperty) extends DMILeafFilter
final case class DMIExistsFilter(property: Seq[String]) extends DMILeafFilter
final case class DMIContainsAnyFilter(property: Seq[String], values: Seq[AnyProperty])
    extends DMILeafFilter
final case class DMIContainsAllFilter(property: Seq[String], values: Seq[AnyProperty])
    extends DMILeafFilter

final case class DataModelInstanceQuery(
    model: DataModelIdentifier,
    filter: Option[DataModelInstanceFilter] = None,
    sort: Option[Seq[String]] = None,
    limit: Option[Int] = None,
    cursor: Option[String] = None
)

final case class DataModelInstanceQueryResponse(
    items: Seq[DataModelInstance],
    modelProperties: Option[Map[String, DataModelPropertyDeffinition]] = None,
    nextCursor: Option[String] = None
)

final case class DataModelInstanceByExternalId(
    items: Seq[String],
    model: DataModelIdentifier
)
