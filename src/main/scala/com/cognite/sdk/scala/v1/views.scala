// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1
import com.cognite.sdk.scala.common.DomainSpecificLanguageFilter
import io.circe.{Codec, Decoder, Encoder}
import io.circe.generic.extras.semiauto.{deriveCodec, deriveEnumerationCodec}
import io.circe.Codec
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto.deriveEnumerationCodec

sealed trait ReferenceType {
 def toString: String
}
case object ViewType extends ReferenceType {
 override def toString: String = "view"
}
case object ContainerType extends ReferenceType {
 override def toString: String = "container"
}
object ReferenceType {
  implicit val referenceTypeCodec: Codec[ReferenceType] =
   deriveEnumerationCodec[ReferenceType]
}

final case class ViewReference(
  space: String,
  externalId: String,
  version: Option[String] = None
) {
 val `type`: ReferenceType =  ViewType
}
object ViewReference {
 implicit val viewReferenceCodec: Codec[ViewReference] =
  deriveEnumerationCodec[ViewReference]
}

final case class ContainerReference(
  space: String,
  externalId: String
) {
 val `type`: ReferenceType = ContainerType
}
object ContainerReference {
 implicit val containerReferenceCodec: Codec[ContainerReference] =
  deriveCodec[ContainerReference]
}

final case class CreatePropertyReference(
   container: ContainerReference,
   externalId: String,
   name: Option[String] = None,
   description: Option[String] = None
 )
object CreatePropertyReference {
 implicit val createPropertyReferenceCodec: Codec[CreatePropertyReference] =
  deriveCodec[CreatePropertyReference]
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


sealed trait FDMTextPropertyType
sealed trait PrimitivePropertyType
sealed trait CDFReferencePropertyType
sealed trait DirectNodeRelationPropertyType
case object Text extends FDMTextPropertyType
case object Boolean extends PrimitivePropertyType
case object Float32 extends PrimitivePropertyType
case object Float64 extends PrimitivePropertyType
case object Int32 extends PrimitivePropertyType
case object Int64 extends PrimitivePropertyType
case object Numeric extends PrimitivePropertyType
case object Timestamp extends PrimitivePropertyType
case object Date extends PrimitivePropertyType
case object Json extends PrimitivePropertyType
case object Resource extends CDFReferencePropertyType
case object Direct extends DirectNodeRelationPropertyType
object PrimitivePropertyType {
 implicit val fdmPropertyCodec: Codec[PrimitivePropertyType] =
  deriveEnumerationCodec[PrimitivePropertyType]
}
object FDMTextPropertyType {
 implicit val fdmPropertyCodec: Codec[FDMTextPropertyType] =
  deriveEnumerationCodec[FDMTextPropertyType]
}

object CDFReferencePropertyType {
 implicit val fdmPropertyCodec: Codec[CDFReferencePropertyType] =
  deriveEnumerationCodec[CDFReferencePropertyType]
}

object DirectNodeRelationPropertyType {
 implicit val fdmPropertyCodec: Codec[DirectNodeRelationPropertyType] =
  deriveEnumerationCodec[DirectNodeRelationPropertyType]
}


sealed trait FDMPropertyInfo
case class TextPropertyInfo(
 `type`: FDMTextPropertyType,
 list: Boolean = false,
 collation: String = "ucs_basic" // todo make enum
) extends FDMPropertyInfo

case class PrimitivePropertyInfo(
  `type`: PrimitivePropertyType,
   list: Boolean = false
) extends FDMPropertyInfo

case class CDFReferenceTypePropertyInfo(
  `type`: CDFReferencePropertyType,
   resource: String, // todo make enum
   list: Boolean = false
) extends FDMPropertyInfo

case class DirectNodePropertyInfo(
   `type`: DirectNodeRelationPropertyType,
   containerExternalId: ContainerReference
) extends FDMPropertyInfo

object FDMPropertyInfo {
 import cats.syntax.functor._
 import io.circe.generic.auto._
 import io.circe.syntax._
 implicit val encodeEvent: Encoder[FDMPropertyInfo] = Encoder.instance {
  case textPropertyInfo: TextPropertyInfo => textPropertyInfo.asJson
  case primitivePropertyInfo: PrimitivePropertyInfo => primitivePropertyInfo.asJson
  case cdfReferenceTypePropertyInfo: CDFReferenceTypePropertyInfo => cdfReferenceTypePropertyInfo.asJson
  case directNodePropertyInfo: DirectNodePropertyInfo => directNodePropertyInfo.asJson
 }

 implicit val decodeEvent: Decoder[FDMPropertyInfo] =
  List[Decoder[FDMPropertyInfo]](
   Decoder[TextPropertyInfo].widen,
   Decoder[PrimitivePropertyInfo].widen,
   Decoder[CDFReferenceTypePropertyInfo].widen,
   Decoder[DirectNodePropertyInfo].widen
  ).reduceLeft(_ or _)
}

final case class ViewPropertyDefinition(
   externalId: String,
   nullable: Boolean = true,
   autoIncrement: Boolean = false,
   defaultValue: Option[DataModelProperty[_]] = None,
   description: Option[String] = None,
   `type`: FDMPropertyInfo,
   container: Option[ContainerReference] = None,
   containerPropertyExternalId: Option[String] = None
 )

final case class DataModelReference(
  space: String,
  externalId: String,
  version: String
)

final case class ViewDefinition(
   space: String,
   externalId: String,
   name: Option[String] = None,
   description: Option[String] = None,
   filter: Option[DomainSpecificLanguageFilter] = None,
   implements: Option[Seq[ViewReference]] = None,
   version: Option[String] = None,
   properties: Map[String, ViewPropertyDefinition],
   usedBy: Option[Seq[DataModelReference]] = None
)
