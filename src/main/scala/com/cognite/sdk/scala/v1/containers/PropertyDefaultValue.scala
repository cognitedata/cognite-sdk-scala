package com.cognite.sdk.scala.v1.containers

import io.circe._
import io.circe.generic.extras.semiauto.{deriveEnumerationDecoder, deriveEnumerationEncoder}

sealed trait PropertyDefaultValue

object PropertyDefaultValue {
  case object String extends PropertyDefaultValue

  case object Number extends PropertyDefaultValue

  case object Boolean extends PropertyDefaultValue

  case object Object extends PropertyDefaultValue

  implicit val propertyDefaultValueEncoder: Encoder[PropertyDefaultValue] =
    deriveEnumerationEncoder[PropertyDefaultValue]

  implicit val propertyDefaultValueDecoder: Decoder[PropertyDefaultValue] =
    deriveEnumerationDecoder[PropertyDefaultValue]
}
