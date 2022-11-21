// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1
import com.cognite.sdk.scala.common.DomainSpecificLanguageFilter
import com.cognite.sdk.scala.v1.containers.{ContainerPropertyType, ContainerReference}
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class ViewReference(
    space: String,
    externalId: String,
    version: Option[String] = None
) {
  val `type`: String = "view"
}
object ViewReference {
  implicit val viewReferenceEncoder: Encoder[ViewReference] =
    deriveEncoder[ViewReference]
  implicit val viewReferenceDecoder: Decoder[ViewReference] =
    deriveDecoder[ViewReference]
}

final case class CreatePropertyReference(
    container: ContainerReference,
    externalId: String,
    name: Option[String] = None,
    description: Option[String] = None
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

// TODO Some of the fields are required but they were made optional to work with mock server
final case class ViewPropertyDefinition(
    externalId: String,
    nullable: Option[Boolean] = Some(true),
    autoIncrement: Option[Boolean] = Some(false),
// TODO add later
//   defaultValue: Option[DataModelProperty[_]] = None,
    description: Option[String] = None,
    `type`: Option[ContainerPropertyType] = None,
    container: Option[ContainerReference] = None,
    containerPropertyExternalId: Option[String] = None
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
// TODO Add later
//   filter: Option[DomainSpecificLanguageFilter] = None,
    implements: Option[Seq[ViewReference]] = None,
    version: Option[String] = None,
    properties: Map[String, ViewPropertyDefinition],
    usedBy: Option[Seq[DataModelReference]] = None
)
