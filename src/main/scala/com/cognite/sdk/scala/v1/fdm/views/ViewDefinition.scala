// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1.fdm.views

import com.cognite.sdk.scala.v1.fdm.common.Usage
import com.cognite.sdk.scala.v1.fdm.common.filters.FilterDefinition
import com.cognite.sdk.scala.v1.fdm.common.properties.PropertyDefinition.ViewPropertyDefinition
import com.cognite.sdk.scala.v1.fdm.common.sources.SourceDefinition
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

final case class ViewDefinition(
    space: String,
    externalId: String,
    name: Option[String] = None,
    description: Option[String] = None,
    filter: Option[FilterDefinition] = None,
    implements: Option[Seq[ViewReference]] = None,
    version: String,
    createdTime: Long,
    lastUpdatedTime: Long,
    writable: Boolean,
    usedFor: Usage,
    properties: Map[String, ViewPropertyDefinition]
) extends SourceDefinition {
  override def toSourceReference: ViewReference =
    ViewReference(space = space, externalId = externalId, version = version)
}

object ViewDefinition {
  implicit val viewDefinitionEncoder: Encoder[ViewDefinition] = deriveEncoder[ViewDefinition]
  implicit val viewDefinitionDecoder: Decoder[ViewDefinition] = deriveDecoder[ViewDefinition]
}
