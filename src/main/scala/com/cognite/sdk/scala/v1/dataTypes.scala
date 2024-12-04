// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1

import cats.implicits._
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

import java.time.Instant

sealed trait CogniteIdOrInstanceId
sealed trait CogniteId extends CogniteIdOrInstanceId

final case class CogniteExternalId(externalId: String) extends CogniteId
final case class CogniteInternalId(id: Long) extends CogniteId
final case class InstanceId(space: String, externalId: String) extends CogniteIdOrInstanceId

object CogniteExternalId {
  implicit val encoder: Encoder[CogniteExternalId] = deriveEncoder
  implicit val decoder: Decoder[CogniteExternalId] = deriveDecoder
}
object CogniteInternalId {
  implicit val encoder: Encoder[CogniteInternalId] = deriveEncoder
  implicit val decoder: Decoder[CogniteInternalId] = deriveDecoder
}

object InstanceId {
  implicit val encoder: Encoder[InstanceId] = deriveEncoder
  implicit val decoder: Decoder[InstanceId] = deriveDecoder
}

object CogniteIdOrInstanceId {
  implicit val encoder: Encoder[CogniteIdOrInstanceId] = Encoder.instance {
    case id: CogniteId => CogniteId.encoder(id)
    case id: InstanceId => InstanceId.encoder(id)
  }
  @SuppressWarnings(
    Array("org.wartremover.warts.TraversableOps")
  )
  implicit val decoder: Decoder[CogniteIdOrInstanceId] =
    List[Decoder[CogniteIdOrInstanceId]](
      Decoder[CogniteId].widen,
      Decoder[InstanceId].widen
    ).reduceLeftOption(_ or _).getOrElse(Decoder[CogniteId].widen)
}

object CogniteId {
  implicit val encoder: Encoder[CogniteId] = Encoder.instance {
    case id: CogniteExternalId => CogniteExternalId.encoder(id)
    case id: CogniteInternalId => CogniteInternalId.encoder(id)
  }
  @SuppressWarnings(
    Array("org.wartremover.warts.TraversableOps")
  )
  implicit val decoder: Decoder[CogniteId] =
    List[Decoder[CogniteId]](
      Decoder[CogniteInternalId].widen,
      Decoder[CogniteExternalId].widen
    ).reduceLeftOption(_ or _).getOrElse(Decoder[CogniteExternalId].widen)
}

// min and max need to be optional, since one of them can be provided alone.
final case class TimeRange(min: Option[Instant] = None, max: Option[Instant] = None)
final case class ConfidenceRange(min: Option[Double] = None, max: Option[Double] = None)

// Used for filtering by label, labels: {containsAny: [{externalId: "label1"}, {externalId: "label2}]}
// or labels: {containsAll: [{externalId: "label1"}, {externalId: "label2}]}
sealed trait LabelContainsFilter
final case class ContainsAny(containsAny: Seq[CogniteExternalId]) extends LabelContainsFilter
final case class ContainsAll(containsAll: Seq[CogniteExternalId]) extends LabelContainsFilter
