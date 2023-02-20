package com.cognite.sdk.scala.v1.fdm.common.properties

import cats.implicits.toFunctorOps
import com.cognite.sdk.scala.v1.fdm.common.DirectRelationReference
import com.cognite.sdk.scala.v1.fdm.containers.ContainerReference
import com.cognite.sdk.scala.v1.fdm.views.{ConnectionDirection, ViewReference}
import io.circe._
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.syntax.EncoderOps

sealed trait PropertyDefinition

object PropertyDefinition {
  sealed trait CorePropertyDefinition extends PropertyDefinition {
    val nullable: Option[Boolean]
    val autoIncrement: Option[Boolean]
    val defaultValue: Option[PropertyDefaultValue]
    val description: Option[String]
    val name: Option[String]
    val `type`: PropertyType
  }

  sealed trait ViewPropertyDefinition extends PropertyDefinition

  final case class ContainerPropertyDefinition(
      nullable: Option[Boolean] = Some(true),
      autoIncrement: Option[Boolean] = Some(false),
      defaultValue: Option[PropertyDefaultValue],
      description: Option[String],
      name: Option[String],
      `type`: PropertyType
  ) extends CorePropertyDefinition

  final case class ViewCorePropertyDefinition(
      nullable: Option[Boolean] = Some(true),
      autoIncrement: Option[Boolean] = Some(false),
      defaultValue: Option[PropertyDefaultValue],
      description: Option[String] = None,
      name: Option[String] = None,
      `type`: PropertyType,
      container: Option[ContainerReference] = None,
      containerPropertyIdentifier: Option[String] = None
  ) extends ViewPropertyDefinition
      with CorePropertyDefinition

  final case class ConnectionDefinition(
      name: Option[String],
      description: Option[String],
      `type`: DirectRelationReference,
      source: ViewReference,
      direction: Option[ConnectionDirection]
  ) extends ViewPropertyDefinition

  implicit val viewCorePropertyDefinitionEncoder: Encoder[ViewCorePropertyDefinition] =
    deriveEncoder[ViewCorePropertyDefinition]

  implicit val containerPropertyDefinitionEncoder: Encoder[ContainerPropertyDefinition] =
    deriveEncoder[ContainerPropertyDefinition]

  implicit val connectionDefinitionEncoder: Encoder[ConnectionDefinition] =
    deriveEncoder[ConnectionDefinition]

  implicit val propertyDefinitionEncoder: Encoder[CorePropertyDefinition] = Encoder.instance {
    case c: ContainerPropertyDefinition => c.asJson
    case v: ViewCorePropertyDefinition => v.asJson
  }

  implicit val viewPropertyDefinitionEncoder: Encoder[ViewPropertyDefinition] = Encoder.instance {
    case c: ViewCorePropertyDefinition => c.asJson
    case v: ConnectionDefinition => v.asJson
  }

  private val derivedViewPropertyDefinitionDecoder: Decoder[ViewCorePropertyDefinition] =
    deriveDecoder[ViewCorePropertyDefinition]

  implicit val viewPropertyDefinitionWithTypeBasedDefaultValue
      : Decoder[ViewCorePropertyDefinition] =
    Decoder.instance[ViewCorePropertyDefinition] { (c: HCursor) =>
      derivedViewPropertyDefinitionDecoder(c)
        .map { p =>
          p.copy(defaultValue =
            propertyTypeBasedPropertyDefaultValue(
              p.`type`,
              c.downField("defaultValue").as[Option[Json]].getOrElse(None)
            )
          )
        }
    }

  private val derivedContainerPropertyDefinitionDecoder: Decoder[ContainerPropertyDefinition] =
    deriveDecoder[ContainerPropertyDefinition]

  implicit val containerPropertyDefinitionDecoderWithTypeBasedDefaultValue
      : Decoder[ContainerPropertyDefinition] = Decoder.instance[ContainerPropertyDefinition] {
    (c: HCursor) =>
      derivedContainerPropertyDefinitionDecoder(c)
        .map { p =>
          p.copy(defaultValue =
            propertyTypeBasedPropertyDefaultValue(
              p.`type`,
              c.downField("defaultValue").as[Option[Json]].getOrElse(None)
            )
          )
        }
  }

  implicit val connectionDefinitionDecoder: Decoder[ConnectionDefinition] =
    deriveDecoder[ConnectionDefinition]

  // scalastyle:off cyclomatic.complexity
  private def propertyTypeBasedPropertyDefaultValue(
      propType: PropertyType,
      defaultValueJson: Option[Json]
  ): Option[PropertyDefaultValue] = {
    val defaultValue = defaultValueJson.flatMap { json =>
      propType match {
        case PropertyType.TextProperty(None | Some(false), _) =>
          json.asString.map(PropertyDefaultValue.String.apply)
        case PropertyType.PrimitiveProperty(PrimitivePropType.Boolean, _) =>
          json.asBoolean.map(PropertyDefaultValue.Boolean.apply)
        case PropertyType.PrimitiveProperty(PrimitivePropType.Int32, None | Some(false)) =>
          json.asNumber.flatMap(_.toInt).map(PropertyDefaultValue.Int32.apply)
        case PropertyType.PrimitiveProperty(PrimitivePropType.Int64, None | Some(false)) =>
          json.asNumber.flatMap(_.toLong).map(PropertyDefaultValue.Int64.apply)
        case PropertyType.PrimitiveProperty(PrimitivePropType.Float32, None | Some(false)) =>
          json.asNumber.map(v => PropertyDefaultValue.Float32(v.toFloat))
        case PropertyType.PrimitiveProperty(PrimitivePropType.Float64, None | Some(false)) =>
          json.asNumber.map(v => PropertyDefaultValue.Float64(v.toDouble))
        case PropertyType.PrimitiveProperty(PrimitivePropType.Date, None | Some(false)) =>
          json.asString.map(PropertyDefaultValue.String.apply)
        case PropertyType.PrimitiveProperty(PrimitivePropType.Timestamp, None | Some(false)) =>
          json.asString.map(PropertyDefaultValue.String.apply)
        case PropertyType.PrimitiveProperty(PrimitivePropType.Json, None | Some(false)) =>
          Some(PropertyDefaultValue.Object(json))
        case _: PropertyType.DirectNodeRelationProperty =>
          Some(PropertyDefaultValue.Object(json))
        case _ => None
      }
    }
    defaultValue
  }
  // scalastyle:on cyclomatic.complexity

  implicit val propertyDefinitionDecoder: Decoder[CorePropertyDefinition] =
    List[Decoder[CorePropertyDefinition]](
      Decoder[ViewCorePropertyDefinition].widen,
      Decoder[ContainerPropertyDefinition].widen
    ).reduceLeftOption(_ or _).getOrElse(Decoder[ViewCorePropertyDefinition].widen)

  implicit val viewPropertyDefinitionDecoder: Decoder[ViewPropertyDefinition] =
    List[Decoder[ViewPropertyDefinition]](
      Decoder[ViewCorePropertyDefinition].widen,
      Decoder[ConnectionDefinition].widen
    ).reduceLeftOption(_ or _).getOrElse(Decoder[ViewCorePropertyDefinition].widen)
}
