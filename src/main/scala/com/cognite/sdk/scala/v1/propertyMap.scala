package com.cognite.sdk.scala.v1

sealed class PropertyMap(val allProperties: Map[String, DataModelProperty[_]]) {
  val externalId: String = allProperties.get("externalId") match {
    case Some(PropertyType.Text.Property(externalId)) => externalId
    case Some(invalidType) =>
      throw new Exception(
        s"externalId should be a TextProperty, it is ${invalidType.toString} instead"
      )
    case None =>
      throw new Exception("externalId is required")
  }
}

final case class Node(
    override val externalId: String,
    `type`: Option[String] = None,
    name: Option[String] = None,
    description: Option[String] = None,
    properties: Option[Map[String, DataModelProperty[_]]] = None
) extends PropertyMap(
      {
        val propsToAdd: Seq[Option[(String, DataModelProperty[_])]] =
          Seq[(String, Option[DataModelProperty[_]])](
            "externalId" -> Some(PropertyType.Text.Property(externalId)),
            "type" -> `type`.map(PropertyType.Text.Property(_)),
            "name" -> name.map(PropertyType.Text.Property(_)),
            "description" -> description.map(PropertyType.Text.Property(_))
          ).map { case (k, v) =>
            v.map(k -> _)
          }

        properties.getOrElse(Map.empty[String, DataModelProperty[_]]) ++
          propsToAdd.flatten.toMap
      }
    )

final case class DataModelNodeCreate(
    spaceExternalId: String,
    model: DataModelIdentifier,
    overwrite: Boolean = false,
    items: Seq[PropertyMap]
)

sealed trait DomainSpecificLanguageFilter

case object EmptyFilter extends DomainSpecificLanguageFilter

sealed trait DMIBoolFilter extends DomainSpecificLanguageFilter
final case class DMIAndFilter(and: Seq[DomainSpecificLanguageFilter]) extends DMIBoolFilter
final case class DMIOrFilter(or: Seq[DomainSpecificLanguageFilter]) extends DMIBoolFilter
final case class DMINotFilter(not: DomainSpecificLanguageFilter) extends DMIBoolFilter

sealed trait DMILeafFilter extends DomainSpecificLanguageFilter
final case class DMIEqualsFilter(property: Seq[String], value: DataModelProperty[_])
    extends DMILeafFilter
final case class DMIInFilter(property: Seq[String], values: Seq[DataModelProperty[_]])
    extends DMILeafFilter
final case class DMIRangeFilter(
    property: Seq[String],
    gte: Option[DataModelProperty[_]] = None,
    gt: Option[DataModelProperty[_]] = None,
    lte: Option[DataModelProperty[_]] = None,
    lt: Option[DataModelProperty[_]] = None
) extends DMILeafFilter {
  require(
    !(gte.isDefined && gt.isDefined) && // can't have both upper bound in the same time
      !(lte.isDefined && lt.isDefined) && // can't have both lower bound in the same time
      (gte.isDefined || gt.isDefined || lte.isDefined || lt.isDefined) // at least one bound must be defined
  )
}
final case class DMIPrefixFilter(property: Seq[String], value: DataModelProperty[_])
    extends DMILeafFilter
final case class DMIExistsFilter(property: Seq[String]) extends DMILeafFilter
final case class DMIContainsAnyFilter(property: Seq[String], values: Seq[DataModelProperty[_]])
    extends DMILeafFilter
final case class DMIContainsAllFilter(property: Seq[String], values: Seq[DataModelProperty[_]])
    extends DMILeafFilter

final case class DataModelInstanceQuery(
    model: DataModelIdentifier,
    filter: DomainSpecificLanguageFilter = EmptyFilter,
    sort: Option[Seq[String]] = None,
    limit: Option[Int] = None,
    cursor: Option[String] = None
)

final case class DataModelInstanceQueryResponse(
    items: Seq[PropertyMap],
    modelProperties: Option[Map[String, DataModelPropertyDefinition]] = None,
    nextCursor: Option[String] = None
)

final case class DataModelInstanceByExternalId(
    items: Seq[CogniteExternalId],
    model: DataModelIdentifier
)
