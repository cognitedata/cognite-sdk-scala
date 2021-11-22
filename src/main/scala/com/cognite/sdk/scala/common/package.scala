// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala

import java.time.Instant
import com.cognite.sdk.scala.v1.{CogniteExternalId, CogniteId, CogniteInternalId, ContainsAll, ContainsAny, LabelContainsFilter, TimeRange}
import com.github.plokhotnyuk.jsoniter_scala.core.{JsonReader, JsonReaderException, JsonValueCodec, JsonWriter}
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker

package object common {
  private val cogniteInternalIdCodec: JsonValueCodec[CogniteInternalId] = JsonCodecMaker.make
  private val cogniteExternalIdCodec: JsonValueCodec[CogniteExternalId] = JsonCodecMaker.make
  implicit val cogniteIdCodec: JsonValueCodec[CogniteId] = new JsonValueCodec[CogniteId] {
    override def decodeValue(in: JsonReader, default: CogniteId): CogniteId =
      try {
        in.setMark()
        cogniteInternalIdCodec.decodeValue(in, null)
      } catch {
        case _: JsonReaderException =>
          in.rollbackToMark()
          cogniteExternalIdCodec.decodeValue(in, null)
      }

    override def encodeValue(x: CogniteId, out: JsonWriter): Unit = x match {
      case e: CogniteExternalId => cogniteExternalIdCodec.encodeValue(e, out)
      case i: CogniteInternalId => cogniteInternalIdCodec.encodeValue(i, out)
    }

    override def nullValue: CogniteId = null
  }
  implicit val cogniteIdItemsCodec: JsonValueCodec[Items[CogniteId]] = JsonCodecMaker.make
  implicit val cogniteInternalIdItemsCodec: JsonValueCodec[Items[CogniteInternalId]] = JsonCodecMaker.make
  implicit val cogniteExternalIdItemsCodec: JsonValueCodec[Items[CogniteExternalId]] = JsonCodecMaker.make

  implicit val labelContainsCodec: JsonValueCodec[LabelContainsFilter] = new JsonValueCodec[LabelContainsFilter] {
    override def decodeValue(in: JsonReader, default: LabelContainsFilter): LabelContainsFilter = ???

    override def encodeValue(x: LabelContainsFilter, out: JsonWriter): Unit = {
      out.writeObjectStart()
      val externalIds = x match {
        case ContainsAny(externalIds) =>
          out.writeKey("containsAny")
          externalIds
        case ContainsAll(externalIds) =>
          out.writeKey("containsAll")
          externalIds
      }
      out.writeArrayStart()
      externalIds.foreach { id =>
        out.writeObjectStart()
        out.writeKey("externalId")
        out.writeVal(id.externalId)
        out.writeObjectEnd()
      }
      out.writeArrayEnd()
      out.writeObjectEnd()
    }

    override def nullValue: LabelContainsFilter = null
  }
  implicit val containsAnyCodec: JsonValueCodec[ContainsAny] = JsonCodecMaker.make
  implicit val containsAllCodec: JsonValueCodec[ContainsAll] = JsonCodecMaker.make

  implicit val instantCodec: JsonValueCodec[Instant] = new JsonValueCodec[Instant] {
    override def decodeValue(in: JsonReader, default: Instant): Instant = Instant.ofEpochMilli(in.readLong())

    override def encodeValue(x: Instant, out: JsonWriter): Unit = out.writeVal(x.toEpochMilli)

    override def nullValue: Instant = Instant.EPOCH
  }
  implicit val timeRangeCodec: JsonValueCodec[TimeRange] = JsonCodecMaker.make

  implicit val unitCodec: JsonValueCodec[Unit] = new JsonValueCodec[Unit] {
    override def decodeValue(in: JsonReader, default: Unit): Unit = {
      in.setMark()
      if (in.nextToken() == '{' && in.nextToken() == '}') {
        ()
      } else {
        in.rollbackToMark()
        in.decodeError("Expected empty JSON object")
      }
    }

    override def encodeValue(x: Unit, out: JsonWriter): Unit = {
      out.writeObjectStart()
      out.writeObjectEnd()
    }

    override def nullValue: Unit = ()
  }
  implicit val itemsWithIgnoreUnknownIdsCodec: JsonValueCodec[ItemsWithIgnoreUnknownIds[CogniteId]] =
    JsonCodecMaker.make
}
