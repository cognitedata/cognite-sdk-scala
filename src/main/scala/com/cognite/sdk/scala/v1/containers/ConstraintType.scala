package com.cognite.sdk.scala.v1.containers

import io.circe._
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto._

sealed trait ConstraintType

object ConstraintType {
  case object Unique extends ConstraintType

  case object Required extends ConstraintType

  implicit val configuration: Configuration = Configuration.default.copy(transformMemberNames = _.toLowerCase, transformConstructorNames = _.toLowerCase)

  implicit val constraintTypeCodec: Encoder[ConstraintType] =
    deriveEnumerationEncoder[ConstraintType]

  implicit val constraintTypeDecoder: Decoder[ConstraintType] =
    deriveEnumerationDecoder[ConstraintType]
}
