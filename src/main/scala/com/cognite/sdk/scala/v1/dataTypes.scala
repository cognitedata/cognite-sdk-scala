// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1

import cats.implicits._
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

import java.time.Instant

sealed trait CogniteId

final case class CogniteExternalId(externalId: String) extends CogniteId
final case class CogniteInternalId(id: Long) extends CogniteId

object CogniteExternalId {
  implicit val encoder: Encoder[CogniteExternalId] = deriveEncoder
  implicit val decoder: Decoder[CogniteExternalId] = deriveDecoder
}
object CogniteInternalId {
  implicit val encoder: Encoder[CogniteInternalId] = deriveEncoder
  implicit val decoder: Decoder[CogniteInternalId] = deriveDecoder
}
object CogniteId {
  implicit val encoder: Encoder[CogniteId] = Encoder.instance {
    case id @ CogniteExternalId(_) => CogniteExternalId.encoder(id)
    case id @ CogniteInternalId(_) => CogniteInternalId.encoder(id)
  }
  @SuppressWarnings(
    Array("org.wartremover.warts.TraversableOps")
  )
  implicit val decoder: Decoder[CogniteId] =
    List[Decoder[CogniteId]](
      Decoder[CogniteInternalId].widen,
      Decoder[CogniteExternalId].widen
    ).reduceLeft(_ or _)
}

// min and max need to be optional, since one of them can be provided alone.
final case class TimeRange(min: Option[Instant] = None, max: Option[Instant] = None)
final case class ConfidenceRange(min: Option[Double] = None, max: Option[Double] = None)

// Used for updating labels on a data type (only assets for now), examples:
// labels: {add: [{externalId: "label1"}, {externalId: "label2}]}, remove: [{externalId: "label3"}]}
// labels: {add: [{externalId: "label1"}, {externalId: "label2}]}}
final case class LabelsOnUpdate(
    add: Option[Seq[CogniteExternalId]] = None,
    remove: Option[Seq[CogniteExternalId]] = None
)

// Used for filtering by label, labels: {containsAny: [{externalId: "label1"}, {externalId: "label2}]}
// or labels: {containsAll: [{externalId: "label1"}, {externalId: "label2}]}
sealed trait LabelContainsFilter
final case class ContainsAny(containsAny: Seq[CogniteExternalId]) extends LabelContainsFilter
final case class ContainsAll(containsAll: Seq[CogniteExternalId]) extends LabelContainsFilter
