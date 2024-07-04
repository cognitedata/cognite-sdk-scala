package com.cognite.sdk.scala.v1

import cats.implicits.catsSyntaxEq
import com.cognite.sdk.scala.common.DomainSpecificLanguageFilter.propEncoder
import com.cognite.sdk.scala.common.SdkException
import io.circe.CursorOp.DownField
import io.circe.{Decoder, DecodingFailure, Encoder, HCursor, Json, KeyEncoder}

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
      case Left(DecodingFailure("Missing required field", downfields)) =>
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
            dataModelPropertyToPropertyValue(c, prop, dmp).map(p => prop -> p)
          }

        filterOutNullableProps(res, props).find(_.isLeft) match {
          case Some(Left(x)) => Left(x)
          case _ => Right(new PropertyMap(res.collect { case Right(value) => value }.toMap))
        }
      }
    }

  private def dataModelPropertyToPropertyValue(
      c: HCursor,
      prop: String,
      dmp: DataModelPropertyDefinition
  ) =
    dmp.`type` match {
      case PropertyType.Text =>
        c.downField(prop)
          .as[Json]
          .map(asStringValue)
          .flatMap {
            case Some(stringVal) => Right(PropertyType.Text.Property(stringVal))
            case None => Left(DecodingFailure(s"Expecting a string", c.history))
          }

      case PropertyType.Array.Text =>
        c.downField(prop)
          .as[Seq[Json]]
          .map(_.flatMap(asStringValue))
          .map(js => PropertyType.Array.Text.Property(js))
      case PropertyType.Json =>
        c.downField(prop).as[Json].map(js => PropertyType.Json.Property(js.toString()))
      case PropertyType.Array.Json =>
        c.downField(prop)
          .as[Seq[Json]]
          .map(js => PropertyType.Array.Json.Property(js.map(_.toString())))
      case _ => dmp.`type`.decodeProperty(c.downField(prop))
    }

  private def asStringValue(json: Json): Option[String] =
    json match {
      case js if js.isString => js.asString
      case js if js.isNumber => js.asNumber.map(_.toString)
      case js if js.isBoolean => js.asBoolean.map(_.toString)
      case js if js.isObject => Some(js.noSpaces)
      case js if js.isArray => js.asArray.map(_.mkString(","))
      case js => Some(js.toString())
    }
}
