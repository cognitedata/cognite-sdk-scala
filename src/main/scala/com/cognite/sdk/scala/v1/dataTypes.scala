// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1

import com.github.plokhotnyuk.jsoniter_scala.core.{JsonReader, JsonValueCodec, JsonWriter}
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker

import java.time.Instant

sealed trait CogniteId

final case class CogniteExternalId(externalId: String) extends CogniteId
final case class CogniteInternalId(id: Long) extends CogniteId

object CogniteExternalId {
  implicit val codec: JsonValueCodec[CogniteExternalId] = JsonCodecMaker.make
}
object CogniteInternalId {
  implicit val codec: JsonValueCodec[CogniteInternalId] = JsonCodecMaker.make
}
object CogniteId {
  // scalastyle:off cyclomatic.complexity
  implicit val codec: JsonValueCodec[CogniteId] = new JsonValueCodec[CogniteId] {
    override def decodeValue(in: JsonReader, default: CogniteId): CogniteId = {
      if (in.isNextToken('{')) {
        var id: Option[Long] = None
        var externalId: Option[String] = None
        if (!in.isNextToken('}')) {
          in.rollbackToken()
          var l = -1
          while (l < 0 || in.isNextToken(',')) {
            l = in.readKeyAsCharBuf()
            if (in.isCharBufEqualsTo(l, "id")) {
              id = Some(in.readLong())
            } else if (in.isCharBufEqualsTo(l, "externalId")) {
              externalId = Some(in.readString(null)) // scalastyle:ignore null
            } else {
              in.skip()
            }
          }
          if (!in.isCurrentToken('}')) {
            in.objectEndOrCommaError()
          }
          (id, externalId) match {
            case (Some(internalId), None) =>
              CogniteInternalId(internalId)
            case (None, Some(externalId)) =>
              CogniteExternalId(externalId)
            case (Some(_), Some(_)) =>
              in.decodeError("Both 'id' and 'externalId' found")
          }
        } else {
          in.readNullOrTokenError(default, '{')
        }
      } else {
        in.readNullOrTokenError(default, '{')
      }
    }

    override def encodeValue(x: CogniteId, out: JsonWriter): Unit = x match {
      case CogniteExternalId(externalId) =>
        out.writeObjectStart()
        out.writeKey("externalId")
        out.writeVal(externalId)
        out.writeObjectEnd()
      case CogniteInternalId(id) =>
        out.writeObjectStart()
        out.writeKey("id")
        out.writeVal(id)
        out.writeObjectEnd()
    }

    override def nullValue: CogniteId = null
  }
}

// min and max need to be optional, since one of them can be provided alone.
final case class TimeRange(min: Option[Instant] = None, max: Option[Instant] = None)
final case class ConfidenceRange(min: Option[Double] = None, max: Option[Double] = None)

// Used for filtering by label, labels: {containsAny: [{externalId: "label1"}, {externalId: "label2}]}
// or labels: {containsAll: [{externalId: "label1"}, {externalId: "label2}]}
sealed trait LabelContainsFilter
final case class ContainsAny(containsAny: Seq[CogniteExternalId]) extends LabelContainsFilter
final case class ContainsAll(containsAll: Seq[CogniteExternalId]) extends LabelContainsFilter
