package com.cognite.sdk.scala.v1.fdm.views

import cats.implicits.toFunctorOps
import com.cognite.sdk.scala.v1.fdm.common.DirectRelationReference
import com.cognite.sdk.scala.v1.fdm.containers.ContainerReference
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.syntax.EncoderOps

sealed trait ViewPropertyCreateDefinition

object ViewPropertyCreateDefinition {
  final case class CreateViewProperty(
      name: Option[String] = None,
      description: Option[String] = None,
      container: ContainerReference,
      containerPropertyIdentifier: String
  ) extends ViewPropertyCreateDefinition

  implicit val createViewPropertyEncoder: Encoder[CreateViewProperty] =
    deriveEncoder[CreateViewProperty]
  implicit val createViewPropertyDecoder: Decoder[CreateViewProperty] =
    deriveDecoder[CreateViewProperty]

  final case class ConnectionDefinition(
      name: Option[String],
      description: Option[String],
      `type`: DirectRelationReference,
      source: ViewReference,
      direction: Option[ConnectionDirection]
  ) extends ViewPropertyCreateDefinition

  implicit val connectionDefinitionEncoder: Encoder[ConnectionDefinition] =
    deriveEncoder[ConnectionDefinition]
  implicit val connectionDefinitionDecoder: Decoder[ConnectionDefinition] =
    deriveDecoder[ConnectionDefinition]

  implicit val viewPropertyEncoder: Encoder[ViewPropertyCreateDefinition] = Encoder.instance {
    case p: CreateViewProperty => p.asJson
    case p: ConnectionDefinition => p.asJson
  }
  implicit val viewPropertyDecoder: Decoder[ViewPropertyCreateDefinition] =
    List[Decoder[ViewPropertyCreateDefinition]](
      Decoder[ConnectionDefinition].widen,
      Decoder[CreateViewProperty].widen
    ).reduceLeftOption(_ or _).getOrElse(Decoder[CreateViewProperty].widen)
}
