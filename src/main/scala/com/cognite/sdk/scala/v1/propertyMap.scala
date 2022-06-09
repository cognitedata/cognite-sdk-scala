package com.cognite.sdk.scala.v1

import cats.implicits.catsSyntaxEq
import com.cognite.sdk.scala.common.DomainSpecificLanguageFilter.propEncoder
import com.cognite.sdk.scala.common.{DomainSpecificLanguageFilter, EmptyFilter}
import io.circe.CursorOp.DownField
import io.circe.{Decoder, DecodingFailure, Encoder, HCursor, KeyEncoder}

import scala.collection.immutable

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

object PropertyMap {
  implicit val dataModelPropertyMapEncoder: Encoder[PropertyMap] =
    Encoder
      .encodeMap(KeyEncoder.encodeKeyString, propEncoder)
      .contramap[PropertyMap](dmi => dmi.allProperties)

  @SuppressWarnings(
    Array("org.wartremover.warts.AsInstanceOf", "org.wartremover.warts.IsInstanceOf")
  )
  private def filterOutNullableProps(
      res: Iterable[Either[DecodingFailure, (String, DataModelProperty[_])]],
      props: Map[String, DataModelPropertyDefinition]
  ): Iterable[Either[DecodingFailure, (String, DataModelProperty[_])]] =
    res.filter {
      case Right(_) => true
      case Left(DecodingFailure("Attempt to decode value on failed cursor", downfields)) =>
        val nullableProps = downfields
          .filter(_.isInstanceOf[DownField])
          .map(_.asInstanceOf[DownField])
          .map(_.k)
          .filter(x => x.neqv("properties") && x.neqv("items"))
          .toSet
        !nullableProps.subsetOf(props.filter(_._2.nullable).keySet)
      case _ => true
    }

  def createDynamicPropertyDecoder(
      props: Map[String, DataModelPropertyDefinition]
  ): Decoder[PropertyMap] =
    new Decoder[PropertyMap] {
      def apply(c: HCursor): Decoder.Result[PropertyMap] = {
        val res: immutable.Iterable[Either[DecodingFailure, (String, DataModelProperty[_])]] =
          props.map { case (prop, dmp) =>
            for {
              value <- dmp.`type`.decodeProperty(c.downField(prop))
            } yield prop -> value
          }
        filterOutNullableProps(res, props).find(_.isLeft) match {
          case Some(Left(x)) => Left(x)
          case _ => Right(new PropertyMap(res.collect { case Right(value) => value }.toMap))
        }
      }
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

final case class Edge(
    override val externalId: String,
    `type`: String,
    startNode: String,
    endNode: String,
    properties: Option[Map[String, DataModelProperty[_]]] = None
) extends PropertyMap(
      {
        val propsToAdd: Seq[Option[(String, DataModelProperty[_])]] =
          Seq[(String, Option[DataModelProperty[_]])](
            "externalId" -> Some(PropertyType.Text.Property(externalId)),
            "type" -> Some(PropertyType.Text.Property(`type`)),
            "startNode" -> Some(PropertyType.Text.Property(startNode)),
            "endNode" -> Some(PropertyType.Text.Property(endNode))
          ).map { case (k, v) =>
            v.map(k -> _)
          }

        properties.getOrElse(Map.empty[String, DataModelProperty[_]]) ++
          propsToAdd.flatten.toMap
      }
    )

final case class EdgeCreate(
    spaceExternalId: String,
    model: DataModelIdentifier,
    autoCreateStartNodes: Boolean = false,
    autoCreateEndNodes: Boolean = false,
    overwrite: Boolean = false,
    items: Seq[PropertyMap]
)
