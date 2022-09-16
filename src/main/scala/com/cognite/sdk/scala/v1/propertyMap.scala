package com.cognite.sdk.scala.v1

import cats.implicits.catsSyntaxEq
import com.cognite.sdk.scala.common.DomainSpecificLanguageFilter.propEncoder
import com.cognite.sdk.scala.common.{DomainSpecificLanguageFilter, EmptyFilter, SdkException}
import com.cognite.sdk.scala.v1.resources.DataModels
import io.circe.CursorOp.DownField
import io.circe.{Decoder, DecodingFailure, Encoder, HCursor, KeyEncoder}

import scala.collection.immutable

sealed class PropertyMap(val allProperties: Map[String, DataModelProperty[_]]) {
  val externalId: String = allProperties.get("externalId") match {
    case Some(PropertyType.Text.Property(externalId)) => externalId
    case Some(invalidType) =>
      throw new SdkException(
        s"externalId should be a TextProperty, but a ${invalidType.toString} was used instead"
      )
    case None =>
      throw new SdkException("externalId is required")
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
    properties: Option[Map[String, DataModelProperty[_]]] = None
) extends PropertyMap(
      {
        val propsToAdd: Map[String, DataModelProperty[_]] =
          Map("externalId" -> PropertyType.Text.Property(externalId))

        properties.map(_ ++ propsToAdd).getOrElse(propsToAdd)
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
    spaceExternalId: String,
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
object DataModelInstanceQueryResponse {
  def createDecoderForQueryResponse(): Decoder[DataModelInstanceQueryResponse] = {
    import DataModels.dataModelPropertyDefinitionDecoder

    (c: HCursor) =>
      for {
        modelProperties <- c
          .downField("modelProperties")
          .as[Map[String, DataModelPropertyDefinition]]

        seqDecoder: Decoder[Seq[PropertyMap]] =
          Decoder.decodeIterable[PropertyMap, Seq](
            PropertyMap.createDynamicPropertyDecoder(modelProperties),
            implicitly
          )

        items <- seqDecoder.tryDecode(c.downField("items"))
        nextCursor <- c.downField("nextCursor").as[Option[String]]
      } yield DataModelInstanceQueryResponse(items, Some(modelProperties), nextCursor)
  }
}

final case class DataModelInstanceByExternalId(
    spaceExternalId: String,
    items: Seq[CogniteExternalId],
    model: DataModelIdentifier
)

final case class Edge(
    override val externalId: String,
    `type`: DirectRelationIdentifier,
    startNode: DirectRelationIdentifier,
    endNode: DirectRelationIdentifier,
    properties: Option[Map[String, DataModelProperty[_]]] = None
) extends PropertyMap(
      {
        def relationIdentifierToPropertyArrayText(
            dri: DirectRelationIdentifier
        ): com.cognite.sdk.scala.v1.PropertyType.Array.Text.Property =
          PropertyType.Array.Text.Property(
            dri.spaceExternalId.map(List(_)).getOrElse(List.empty[String]) ++ List(
              dri.externalId
            )
          )
        val propsToAdd: Map[String, DataModelProperty[_]] = Map[String, DataModelProperty[_]](
          "externalId" -> PropertyType.Text.Property(externalId),
          "type" -> relationIdentifierToPropertyArrayText(`type`),
          "startNode" -> relationIdentifierToPropertyArrayText(startNode),
          "endNode" -> relationIdentifierToPropertyArrayText(endNode)
        )

        properties.map(_ ++ propsToAdd).getOrElse(propsToAdd)
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
