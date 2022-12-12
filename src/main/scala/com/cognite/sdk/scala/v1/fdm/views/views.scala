// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1.fdm.views

import com.cognite.sdk.scala.common.DomainSpecificLanguageFilter
import com.cognite.sdk.scala.v1.fdm.common.filters.FilterDefinition
import com.cognite.sdk.scala.v1.fdm.common.properties.PropertyDefinition.ViewPropertyDefinition
import com.cognite.sdk.scala.v1.fdm.containers.{ContainerReference, ContainerUsage}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

final case class CreatePropertyReference(
    container: ContainerReference,
    containerPropertyIdentifier: String
)
object CreatePropertyReference {
  implicit val createPropertyReferenceEncoder: Encoder[CreatePropertyReference] =
    deriveEncoder[CreatePropertyReference]
  implicit val createPropertyReferenceDecoder: Decoder[CreatePropertyReference] =
    deriveDecoder[CreatePropertyReference]
}

final case class ViewCreateDefinition(
    space: String,
    externalId: String,
    name: Option[String] = None,
    description: Option[String] = None,
    filter: Option[DomainSpecificLanguageFilter] = None,
    implements: Option[Seq[ViewReference]] = None,
    version: Option[String] = None,
    properties: Map[String, CreatePropertyReference]
)

final case class DataModelReference(
    space: String,
    externalId: String,
    version: String
)
object DataModelReference {
  implicit val dataModelReferenceEncoder: Encoder[DataModelReference] =
    deriveEncoder[DataModelReference]
  implicit val dataModelReferenceDecoder: Decoder[DataModelReference] =
    deriveDecoder[DataModelReference]

}

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
    usedFor: ContainerUsage,
    properties: Map[String, ViewPropertyDefinition]
)
