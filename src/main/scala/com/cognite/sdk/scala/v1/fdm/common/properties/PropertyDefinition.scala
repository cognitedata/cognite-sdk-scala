package com.cognite.sdk.scala.v1.fdm.common.properties

import cats.implicits.{catsSyntaxEq, toFunctorOps}
import com.cognite.sdk.scala.v1.fdm.containers.ContainerReference
import io.circe._
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.syntax.EncoderOps

/** Base class for `ContainerPropertyDefinition` & `ViewPropertyDefinition`
  */
sealed trait PropertyDefinition {
  val nullable: Option[Boolean]
  val autoIncrement: Option[Boolean]
  val defaultValue: Option[PropertyDefaultValue]
  val description: Option[String]
  val name: Option[String]
  val `type`: PropertyType

  assert(
    checkDefaultValueAndPropertyTypeCompatibility,
    s"defaultValue: ${defaultValue.map(_.productPrefix).toString} is not compatible with the property type: ${`type`.getClass.getSimpleName}"
  )

  private def checkDefaultValueAndPropertyTypeCompatibility: Boolean = {
    val compatibility = defaultValue
      .map(d => PropertyDefinition.defaultValueCompatibleWithPropertyType(`type`, d))

    compatibility match {
      case Some(true) => true
      case Some(false) if `type`.isList => true
      case None => true
      case _ => false
    }
  }
}

object PropertyDefinition {
  final case class ContainerPropertyDefinition(
      nullable: Option[Boolean] = Some(true),
      autoIncrement: Option[Boolean] = Some(false),
      defaultValue: Option[PropertyDefaultValue],
      description: Option[String],
      name: Option[String],
      `type`: PropertyType
  ) extends PropertyDefinition

  final case class ViewPropertyDefinition(
      nullable: Option[Boolean] = Some(true),
      autoIncrement: Option[Boolean] = Some(false),
      defaultValue: Option[PropertyDefaultValue],
      description: Option[String] = None,
      name: Option[String] = None,
      `type`: PropertyType,
      container: Option[ContainerReference] = None,
      containerPropertyIdentifier: Option[String] = None
  ) extends PropertyDefinition

  // scalastyle:off cyclomatic.complexity
  def defaultValueCompatibleWithPropertyType(
      propertyType: PropertyType,
      defaultValue: PropertyDefaultValue
  ): Boolean =
    (propertyType, defaultValue) match {
      case (p: PropertyType.TextProperty, _: PropertyDefaultValue.String) if !p.isList =>
        true
      case (
            p @ PropertyType.PrimitiveProperty(PrimitivePropType.Int32, _),
            _: PropertyDefaultValue.Int32
          ) if !p.isList =>
        true
      case (
            p @ PropertyType.PrimitiveProperty(PrimitivePropType.Int64, _),
            _: PropertyDefaultValue.Int64
          ) if !p.isList =>
        true
      case (
            p @ PropertyType.PrimitiveProperty(PrimitivePropType.Float32, _),
            _: PropertyDefaultValue.Float32
          ) if !p.isList =>
        true
      case (
            p @ PropertyType.PrimitiveProperty(PrimitivePropType.Float64, _),
            _: PropertyDefaultValue.Float64
          ) if !p.isList =>
        true
      case (
            p @ PropertyType.PrimitiveProperty(PrimitivePropType.Boolean, _),
            _: PropertyDefaultValue.Boolean
          ) if !p.isList =>
        true
      case (
            p @ PropertyType.PrimitiveProperty(
              PrimitivePropType.Timestamp | PrimitivePropType.Date,
              _
            ),
            _: PropertyDefaultValue.String
          ) if !p.isList =>
        true
      case (
            p @ PropertyType.PrimitiveProperty(PrimitivePropType.Json, _),
            _: PropertyDefaultValue.Object
          ) if !p.isList =>
        true
      case _ => false
    }
  // scalastyle:on cyclomatic.complexity

  implicit val viewPropertyDefinitionEncoder: Encoder[ViewPropertyDefinition] =
    deriveEncoder[ViewPropertyDefinition]

  implicit val containerPropertyDefinitionEncoder: Encoder[ContainerPropertyDefinition] =
    deriveEncoder[ContainerPropertyDefinition]

  implicit val propertyDefinitionEncoder: Encoder[PropertyDefinition] = Encoder.instance {
    case c: ContainerPropertyDefinition => c.asJson
    case v: ViewPropertyDefinition => v.asJson
  }

  private val derivedViewPropertyDefinitionDecoder: Decoder[ViewPropertyDefinition] =
    deriveDecoder[ViewPropertyDefinition]

  // decoding without triggering the DefaultValue And PropertyType Compatibility assertion
  implicit val viewPropertyDefinitionWithTypeBasedDefaultValue: Decoder[ViewPropertyDefinition] =
    Decoder.instance[ViewPropertyDefinition] { (c: HCursor) =>
      derivedViewPropertyDefinitionDecoder
        .decodeJson(
          Json.fromJsonObject(
            c.value.asObject
              .map(_.filter { case (k, _) =>
                k =!= "defaultValue"
              })
              .getOrElse(JsonObject.empty)
          )
        )
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

  // decoding without triggering the DefaultValue And PropertyType Compatibility assertion
  implicit val containerPropertyDefinitionDecoderWithTypeBasedDefaultValue
      : Decoder[ContainerPropertyDefinition] = Decoder.instance[ContainerPropertyDefinition] {
    (c: HCursor) =>
      derivedContainerPropertyDefinitionDecoder
        .decodeJson(
          Json.fromJsonObject(
            c.value.asObject
              .map(_.filter { case (k, _) =>
                k =!= "defaultValue"
              })
              .getOrElse(JsonObject.empty)
          )
        )
        .map { p =>
          p.copy(defaultValue =
            propertyTypeBasedPropertyDefaultValue(
              p.`type`,
              c.downField("defaultValue").as[Option[Json]].getOrElse(None)
            )
          )
        }
  }

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
        case PropertyType.DirectNodeRelationProperty(_) =>
          json.asString.map(PropertyDefaultValue.String.apply)
        case _ => None
      }
    }
    defaultValue
  }
  // scalastyle:on cyclomatic.complexity

  implicit val propertyDefinitionDecoder: Decoder[PropertyDefinition] =
    List[Decoder[PropertyDefinition]](
      Decoder[ViewPropertyDefinition].widen,
      Decoder[ContainerPropertyDefinition].widen
    ).reduceLeftOption(_ or _).getOrElse(Decoder[ViewPropertyDefinition].widen)
}
