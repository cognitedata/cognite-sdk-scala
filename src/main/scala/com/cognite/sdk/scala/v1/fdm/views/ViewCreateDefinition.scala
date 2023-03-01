package com.cognite.sdk.scala.v1.fdm.views

import com.cognite.sdk.scala.v1.fdm.common.filters.FilterDefinition
import com.cognite.sdk.scala.v1.fdm.datamodels.DataModelViewReference
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

final case class ViewCreateDefinition(
    space: String,
    externalId: String,
    version: String,
    name: Option[String] = None,
    description: Option[String] = None,
    filter: Option[FilterDefinition] = None,
    implements: Option[Seq[ViewReference]] = None,
    properties: Map[String, ViewPropertyCreateDefinition]
) extends DataModelViewReference

object ViewCreateDefinition {
  implicit val viewCreateDefinitionEncoder: Encoder[ViewCreateDefinition] =
    deriveEncoder[ViewCreateDefinition]
  implicit val viewCreateDefinitionDecoder: Decoder[ViewCreateDefinition] =
    deriveDecoder[ViewCreateDefinition]
}
