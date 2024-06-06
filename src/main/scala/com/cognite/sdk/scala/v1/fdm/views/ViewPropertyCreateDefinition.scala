package com.cognite.sdk.scala.v1.fdm.views

import cats.implicits.toFunctorOps
import com.cognite.sdk.scala.v1.fdm.common.properties.PropertyDefinition
import com.cognite.sdk.scala.v1.fdm.common.properties.PropertyDefinition.{ConnectionDefinition}
import com.cognite.sdk.scala.v1.fdm.containers.ContainerReference
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.syntax.EncoderOps
import io.circe.{Decoder, Encoder}

sealed trait ViewPropertyCreateDefinition

object ViewPropertyCreateDefinition {
  final case class CreateViewProperty(
      name: Option[String] = None,
      description: Option[String] = None,
      container: ContainerReference,
      containerPropertyIdentifier: String
  ) extends ViewPropertyCreateDefinition

  final case class CreateConnectionDefinition(connectionDefinition: ConnectionDefinition)
    extends ViewPropertyCreateDefinition
  implicit val createConnectionDefinitionEncoder: Encoder[CreateConnectionDefinition] =
    PropertyDefinition.connectionDefinitionEncoder.contramap(_.connectionDefinition)
  implicit val createConnectionDefinitionDecoder: Decoder[CreateConnectionDefinition] =
    PropertyDefinition.connectionDefinitionDecoder.map(CreateConnectionDefinition)

  implicit val createViewPropertyEncoder: Encoder[CreateViewProperty] =
    deriveEncoder[CreateViewProperty]
  implicit val createViewPropertyDecoder: Decoder[CreateViewProperty] =
    deriveDecoder[CreateViewProperty]

  implicit val viewPropertyEncoder: Encoder[ViewPropertyCreateDefinition] = Encoder.instance {
    case p: CreateViewProperty => p.asJson
    case d: CreateConnectionDefinition => d.asJson
  }

  implicit val viewPropertyDecoder: Decoder[ViewPropertyCreateDefinition] =
    List[Decoder[ViewPropertyCreateDefinition]](
      Decoder[CreateConnectionDefinition].widen,
      Decoder[CreateViewProperty].widen
    ).reduceLeftOption(_ or _).getOrElse(Decoder[CreateViewProperty].widen)
}
