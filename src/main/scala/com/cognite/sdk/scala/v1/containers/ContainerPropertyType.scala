package com.cognite.sdk.scala.v1.containers

import cats.implicits.toFunctorOps
import io.circe._
import io.circe.generic.semiauto._

sealed trait ContainerPropertyType

object ContainerPropertyType {

  final case class TextProperty(list: Boolean = false, collation: String = "ucs_basic")
      extends ContainerPropertyType {
    val `type` = "text"
  }

  final case class PrimitiveProperty(`type`: PrimitivePropType, list: Boolean = false)
      extends ContainerPropertyType

  final case class DirectNodeRelationProperty(container: Option[ContainerReference])
      extends ContainerPropertyType {
    val `type` = "direct"
  }

  import com.cognite.sdk.scala.v1.resources.Containers.containerReferenceEncoder
  import com.cognite.sdk.scala.v1.resources.Containers.containerReferenceDecoder

  implicit val propertyTypeTextEncoder: Encoder[TextProperty] =
    deriveEncoder[TextProperty]

  implicit val propertyTypeTextDecoder: Decoder[TextProperty] =
    deriveDecoder[TextProperty]

  implicit val primitivePropertyEncoder: Encoder[PrimitiveProperty] =
    deriveEncoder[PrimitiveProperty]

  implicit val primitivePropertyDecoder: Decoder[PrimitiveProperty] =
    deriveDecoder[PrimitiveProperty]

  implicit val directNodeRelationPropertyEncoder: Encoder[DirectNodeRelationProperty] =
    deriveEncoder[DirectNodeRelationProperty]

  implicit val directNodeRelationPropertyDecoder: Decoder[DirectNodeRelationProperty] =
    deriveDecoder[DirectNodeRelationProperty]

  implicit val containerPropertyTypeEncoder: Encoder[ContainerPropertyType] = Encoder.instance {
    case t: TextProperty => Encoder[TextProperty].apply(t)
    case p: PrimitiveProperty => Encoder[PrimitiveProperty].apply(p)
    case d: DirectNodeRelationProperty => Encoder[DirectNodeRelationProperty].apply(d)
  }

  implicit val containerPropertyTypeDecoder: Decoder[ContainerPropertyType] =
    List[Decoder[ContainerPropertyType]](
      Decoder[TextProperty].widen,
      Decoder[PrimitiveProperty].widen,
      Decoder[DirectNodeRelationProperty].widen
    ).reduceLeft(_ or _)
}
