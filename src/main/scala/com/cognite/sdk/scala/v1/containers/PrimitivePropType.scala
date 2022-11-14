package com.cognite.sdk.scala.v1.containers

import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto.{deriveEnumerationDecoder, deriveEnumerationEncoder}
import io.circe.{Decoder, Encoder}

sealed trait PrimitivePropType

object PrimitivePropType {

  case object Boolean extends PrimitivePropType

  case object Float32 extends PrimitivePropType

  case object Float64 extends PrimitivePropType

  case object Int32 extends PrimitivePropType

  case object Int64 extends PrimitivePropType

  case object Numeric extends PrimitivePropType

  case object Timestamp extends PrimitivePropType

  case object Date extends PrimitivePropType

  case object Json extends PrimitivePropType

  implicit val configuration: Configuration = Configuration.default.copy(transformMemberNames = _.toLowerCase, transformConstructorNames = _.toLowerCase)

  implicit val primitivePropertyTypeEncoder: Encoder[PrimitivePropType] =
    deriveEnumerationEncoder[PrimitivePropType]

  implicit val primitivePropertyTypeDecoder: Decoder[PrimitivePropType] =
    deriveEnumerationDecoder[PrimitivePropType]
}
