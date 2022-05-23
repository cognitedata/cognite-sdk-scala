package com.cognite.sdk.scala.v1

import com.cognite.sdk.scala.v1.DataModelProperty._

sealed class DataModelInstance(val properties: Map[String, DataModelProperty])
{
    val externalId: String = properties("externalId") match {
        case TextProperty(externalId) => externalId
        case invalidType => throw new Exception(
          s"externalId should be a TextProperty, it is ${invalidType} instead"
        )
    }
}

object Node
{
    def Apply(
        externalId: String,
        `type`: Option[String] = None,
        name: Option[String] = None,
        description: Option[String] = None,
        properties: Option[Map[String, DataModelProperty]] = None
    ): DataModelInstance = {
        val allProps = properties.getOrElse(Map.empty[String,DataModelProperty]) ++
        Seq[(String, Option[DataModelProperty])](
            "externalId" -> Some(TextProperty(externalId)),
            "type" -> `type`.map(TextProperty(_)),
            "name" -> name.map(TextProperty(_)),
            "description" -> description.map(TextProperty(_))
        ).filter( kv => kv match {
            case (_,v) => v.isDefined
        }).map(kv => kv match {
            case (k,v) => (k, v.get)
        }).toMap

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
    items: Seq[DataModelInstance],
    modelProperties: Option[Map[String, DataModelPropertyDeffinition]] = None,
    cursor: Option[String] = None
)

final case class DataModelInstanceByExternalId(
    items: Seq[String],
    model: DataModelIdentifier
)
