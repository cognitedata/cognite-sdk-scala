// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala

import java.time.Instant
import com.cognite.sdk.scala.v1.{
  CogniteExternalId,
  CogniteId,
  CogniteIdOrInstanceId,
  CogniteInstanceId,
  CogniteInternalId,
  ContainsAll,
  ContainsAny,
  LabelContainsFilter,
  TimeRange
}
import io.circe.{Decoder, Encoder, Json}
import io.circe.generic.semiauto.deriveEncoder

package object common {

  implicit val cogniteIdOrInstanceIdEncoder: Encoder[CogniteIdOrInstanceId] = {
    case cogniteId: CogniteId => cogniteIdEncoder.apply(cogniteId)
    case instance: CogniteInstanceId =>
      Json.obj(
        (
          "instanceId",
          Json.obj(
            ("space", Json.fromString(instance.instanceId.space)),
            ("externalId", Json.fromString(instance.instanceId.externalId))
          )
        )
      )
  }

  implicit val cogniteIdEncoder: Encoder[CogniteId] = (i: CogniteId) =>
    Json.obj(
      i match {
        case i: CogniteInternalId => ("id", Json.fromLong(i.id))
        case e: CogniteExternalId => ("externalId", Json.fromString(e.externalId))
      }
    )
  implicit val cogniteIdOrInstanceIdItemsEncoder: Encoder[Items[CogniteIdOrInstanceId]] =
    deriveEncoder
  implicit val cogniteIdItemsEncoder: Encoder[Items[CogniteId]] = deriveEncoder
  implicit val cogniteInternalIdEncoder: Encoder[CogniteInternalId] = deriveEncoder
  implicit val cogniteInternalIdItemsEncoder: Encoder[Items[CogniteInternalId]] = deriveEncoder
  implicit val cogniteExternalIdEncoder: Encoder[CogniteExternalId] = deriveEncoder
  implicit val cogniteExternalIdItemsEncoder: Encoder[Items[CogniteExternalId]] = deriveEncoder

  implicit val labelContainsEncoder: Encoder[LabelContainsFilter] = {
    case ContainsAny(externalIds) =>
      Json.obj(
        "containsAny" -> Json.arr(
          externalIds.map(s => Json.obj("externalId" -> Json.fromString(s.externalId))): _*
        )
      )
    case ContainsAll(externalIds) =>
      Json.obj(
        "containsAll" -> Json.arr(
          externalIds.map(s => Json.obj("externalId" -> Json.fromString(s.externalId))): _*
        )
      )
  }
  implicit val containsAnyEncoder: Encoder[ContainsAny] = deriveEncoder
  implicit val containsAllEncoder: Encoder[ContainsAll] = deriveEncoder

  implicit val instantEncoder: Encoder[Instant] = Encoder.encodeLong.contramap(_.toEpochMilli)
  implicit val instantDecoder: Decoder[Instant] = Decoder.decodeLong.map(Instant.ofEpochMilli)
  implicit val timeRangeEncoder: Encoder[TimeRange] = deriveEncoder

  implicit val itemsWithIgnoreUnknownIdsEncoder: Encoder[ItemsWithIgnoreUnknownIds[CogniteId]] =
    deriveEncoder
  implicit val itemsWithIgnoreUnknownIdsOrInstanceIdsEncoder
      : Encoder[ItemsWithIgnoreUnknownIds[CogniteIdOrInstanceId]] = deriveEncoder
}
