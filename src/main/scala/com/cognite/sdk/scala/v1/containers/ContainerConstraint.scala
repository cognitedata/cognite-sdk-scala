// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1.containers

import cats.implicits._
import com.cognite.sdk.scala.v1.resources.Containers.{
  containerReferenceDecoder,
  containerReferenceEncoder
}
import io.circe._
import io.circe.generic.semiauto.deriveDecoder

sealed trait ContainerConstraint

object ContainerConstraint {
  // TODO: Handle ViewReference as well
  final case class RequiresConstraint(require: Option[ContainerReference])
      extends ContainerConstraint {
    val constraintType = "requires"
  }
  final case class UniquenessConstraint(properties: Seq[String]) extends ContainerConstraint {
    val constraintType = "uniqueness"
  }

  implicit val requiresConstraintEncoder: Encoder[RequiresConstraint] =
    Encoder.forProduct2("constraintType", "require")((c: RequiresConstraint) =>
      (c.constraintType, c.require)
    )

  implicit val requiresConstraintDecoder: Decoder[RequiresConstraint] =
    deriveDecoder[RequiresConstraint]

  implicit val uniquenessConstraintEncoder: Encoder[UniquenessConstraint] =
    Encoder.forProduct2("constraintType", "properties")((c: UniquenessConstraint) =>
      (c.constraintType, c.properties)
    )

  implicit val uniquenessConstraintDecoder: Decoder[UniquenessConstraint] =
    deriveDecoder[UniquenessConstraint]

  implicit val containerConstraintEncoder: Encoder[ContainerConstraint] = Encoder.instance {
    case r: RequiresConstraint => Encoder[RequiresConstraint].apply(r)
    case u: UniquenessConstraint => Encoder[UniquenessConstraint].apply(u)
  }

  implicit val containerConstraintDecoder: Decoder[ContainerConstraint] =
    List[Decoder[ContainerConstraint]](
      Decoder[RequiresConstraint].widen,
      Decoder[UniquenessConstraint].widen
    ).reduceLeftOption(_ or _).getOrElse(Decoder[RequiresConstraint].widen)

}
