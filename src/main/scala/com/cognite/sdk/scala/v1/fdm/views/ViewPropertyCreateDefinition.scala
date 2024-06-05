package com.cognite.sdk.scala.v1.fdm.views

import cats.implicits.toFunctorOps
import com.cognite.sdk.scala.v1.fdm.common.properties.PropertyDefinition.{
  ConnectionDefinition,
  connectionDefinitionDecoder
}
import com.cognite.sdk.scala.v1.fdm.containers.ContainerReference
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.syntax.EncoderOps
import io.circe.{Decoder, Encoder}

trait ViewPropertyCreateDefinition

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

  implicit val viewPropertyEncoder: Encoder[ViewPropertyCreateDefinition] = Encoder.instance {
    case p: CreateViewProperty => p.asJson
    case d: ConnectionDefinition => d.asJson
    case _ =>
      throw new IllegalStateException(
        "could not encode property into CreateViewProperty or ConnectionDefinition"
      )
  }

  implicit val viewPropertyDecoder: Decoder[ViewPropertyCreateDefinition] =
    List[Decoder[ViewPropertyCreateDefinition]](
      Decoder[ConnectionDefinition].widen,
      Decoder[CreateViewProperty].widen
    ).reduceLeftOption(_ or _).getOrElse(Decoder[CreateViewProperty].widen)
}
