package com.cognite.sdk.scala.v1.fdm.common.properties

import cats.implicits.toFunctorOps
import com.cognite.sdk.scala.v1.fdm.containers.ContainerReference
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.syntax.EncoderOps
import io.circe.{Decoder, Encoder}

/** Base class for `ContainerPropertyDefinition` & `ViewPropertyDefinition`
  */
sealed trait PropertyDefinition {
  val nullable: Option[Boolean]
  val autoIncrement: Option[Boolean]
  val defaultValue: Option[PropertyDefaultValue]
  val description: Option[String]
  val name: Option[String]
  val `type`: PropertyType
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

  implicit val viewPropertyDefinitionEncoder: Encoder[ViewPropertyDefinition] =
    deriveEncoder[ViewPropertyDefinition]

  implicit val containerPropertyDefinitionEncoder: Encoder[ContainerPropertyDefinition] =
    deriveEncoder[ContainerPropertyDefinition]

  implicit val propertyDefinitionEncoder: Encoder[PropertyDefinition] = Encoder.instance {
    case c: ContainerPropertyDefinition => c.asJson
    case v: ViewPropertyDefinition => v.asJson
  }

  implicit val viewPropertyDefinitionDecoder: Decoder[ViewPropertyDefinition] =
    deriveDecoder[ViewPropertyDefinition]

  implicit val containerPropertyDefinitionDecoder: Decoder[ContainerPropertyDefinition] =
    deriveDecoder[ContainerPropertyDefinition]

  implicit val propertyDefinitionDecoder: Decoder[PropertyDefinition] =
    List[Decoder[PropertyDefinition]](
      Decoder[ViewPropertyDefinition].widen,
      Decoder[ContainerPropertyDefinition].widen
    ).reduceLeftOption(_ or _).getOrElse(Decoder[ViewPropertyDefinition].widen)
}
