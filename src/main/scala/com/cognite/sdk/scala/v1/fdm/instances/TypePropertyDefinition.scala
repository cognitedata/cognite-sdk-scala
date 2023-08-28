package com.cognite.sdk.scala.v1.fdm.instances

import cats.implicits.toTraverseOps
import com.cognite.sdk.scala.v1.fdm.common.properties.{
  PrimitivePropType,
  PropertyDefaultValue,
  PropertyType
}
import io.circe.Decoder.Result
import io.circe.{Decoder, DecodingFailure, Encoder, HCursor, Json}
import io.circe.generic.semiauto.deriveEncoder

@deprecated("message", since = "0")
final case class TypePropertyDefinition(
    nullable: Option[Boolean] = Some(true),
    autoIncrement: Option[Boolean] = Some(false),
    defaultValue: Option[PropertyDefaultValue],
    description: Option[String],
    name: Option[String],
    `type`: PropertyType
)

@deprecated("message", since = "0")
object TypePropertyDefinition {

  implicit val typePropertyDefinitionEncoder: Encoder[TypePropertyDefinition] = deriveEncoder

  implicit val typePropertyDefinitionDecoder: Decoder[TypePropertyDefinition] = (c: HCursor) =>
    for {
      nullable <- c.downField("nullable").as[Option[Boolean]]
      autoIncrement <- c.downField("autoIncrement").as[Option[Boolean]]
      defaultValue <- c.downField("defaultValue").as[Option[Json]]
      description <- c.downField("description").as[Option[String]]
      name <- c.downField("name").as[Option[String]]
      propType <- c.downField("type").as[PropertyType]
      compatibleDefaultValue <- toPropertyTypeCompatibleDefaultValueType(c, propType, defaultValue)
    } yield TypePropertyDefinition(
      nullable = nullable,
      autoIncrement = autoIncrement,
      defaultValue = compatibleDefaultValue,
      description = description,
      name = name,
      `type` = propType
    )

  private def toPropertyTypeCompatibleDefaultValueType(
      c: HCursor,
      propType: PropertyType,
      defaultTypeJson: Option[Json]
  ): Result[Option[PropertyDefaultValue]] =
    defaultTypeJson.traverse { json =>
      propType match {
        case PropertyType.PrimitiveProperty(PrimitivePropType.Int32, None | Some(false)) =>
          defaultValueTypeJsonAsInt32(c, json)
        case PropertyType.PrimitiveProperty(PrimitivePropType.Int64, None | Some(false)) =>
          defaultValueTypeJsonAsInt64(c, json)
        case _ => json.as[PropertyDefaultValue]
      }
    }

  private def defaultValueTypeJsonAsInt64(c: HCursor, json: Json) =
    json.asNumber.flatMap(_.toLong) match {
      case Some(n) => Right(PropertyDefaultValue.Int64(n))
      case None =>
        Left(
          DecodingFailure(
            s"Default value type: ${json.noSpaces} is not compatible with the property type: Int64",
            c.history
          )
        )
    }

  private def defaultValueTypeJsonAsInt32(c: HCursor, json: Json) =
    json.asNumber.flatMap(_.toInt) match {
      case Some(n) => Right(PropertyDefaultValue.Int32(n))
      case None =>
        Left(
          DecodingFailure(
            s"Default value type: ${json.noSpaces} is not compatible with the property type: Int32",
            c.history
          )
        )
    }
}
