package com.cognite.sdk.scala.v1

import com.cognite.sdk.scala.common.{DomainSpecificLanguageFilter, EmptyFilter}

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
