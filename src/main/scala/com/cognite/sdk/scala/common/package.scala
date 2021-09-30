// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala

import java.time.Instant

import com.cognite.sdk.scala.v1.{
  CogniteExternalId,
  CogniteId,
  CogniteInternalId,
  ContainsAll,
  ContainsAny,
  LabelContainsFilter,
  TimeRange
}
import io.circe.{Decoder, Encoder, Json}
import io.circe.generic.semiauto.deriveEncoder

package object common {
  implicit val cogniteIdEncoder: Encoder[CogniteId] = new Encoder[CogniteId] {
    final def apply(i: CogniteId): Json = Json.obj(
      i match {
        case i: CogniteInternalId => ("id", Json.fromLong(i.id))
        case e: CogniteExternalId => ("externalId", Json.fromString(e.externalId))
      }
    )
  }
  implicit val cogniteIdItemsEncoder: Encoder[Items[CogniteId]] = deriveEncoder
  implicit val cogniteInternalIdEncoder: Encoder[CogniteInternalId] = deriveEncoder
  implicit val cogniteInternalIdItemsEncoder: Encoder[Items[CogniteInternalId]] = deriveEncoder
  implicit val cogniteExternalIdEncoder: Encoder[CogniteExternalId] = deriveEncoder
  implicit val cogniteExternalIdItemsEncoder: Encoder[Items[CogniteExternalId]] = deriveEncoder

  implicit val labelContainsEncoder: Encoder[LabelContainsFilter] =
    new Encoder[LabelContainsFilter] {
      final def apply(i: LabelContainsFilter): Json = Json.obj(
        i match {
          case ca: ContainsAny =>
            (
              "containsAny",
              Json.arr(
                ca.containsAny.map(s => Json.obj(("externalId", Json.fromString(s.externalId)))): _*
              )
            )
          case conAll: ContainsAll =>
            (
              "containsAll",
              Json.arr(
                conAll.containsAll
                  .map(s => Json.obj(("externalId", Json.fromString(s.externalId)))): _*
              )
            )
        }
      )
    }
  implicit val containsAnyEncoder: Encoder[ContainsAny] = deriveEncoder
  implicit val containsAllEncoder: Encoder[ContainsAll] = deriveEncoder

  implicit val instantEncoder: Encoder[Instant] = Encoder.encodeLong.contramap(_.toEpochMilli)
  implicit val instantDecoder: Decoder[Instant] = Decoder.decodeLong.map(Instant.ofEpochMilli)
  implicit val timeRangeEncoder: Encoder[TimeRange] = deriveEncoder

  implicit val itemsWithIgnoreUnknownIdsEncoder: Encoder[ItemsWithIgnoreUnknownIds[CogniteId]] =
    deriveEncoder
}
