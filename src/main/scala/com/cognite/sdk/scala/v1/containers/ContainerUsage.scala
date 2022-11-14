package com.cognite.sdk.scala.v1.containers

import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto.{deriveEnumerationDecoder, deriveEnumerationEncoder}
import io.circe.{Decoder, Encoder}

sealed trait ContainerUsage

object ContainerUsage {

  case object Node extends ContainerUsage

  case object Edge extends ContainerUsage

  case object All extends ContainerUsage

  implicit val configuration: Configuration = Configuration.default.copy(transformMemberNames = _.toLowerCase, transformConstructorNames = _.toLowerCase)

  implicit val containerUsageEncoder: Encoder[ContainerUsage] =
    deriveEnumerationEncoder[ContainerUsage]

  implicit val containerUsageDecoder: Decoder[ContainerUsage] =
    deriveEnumerationDecoder[ContainerUsage]
}
