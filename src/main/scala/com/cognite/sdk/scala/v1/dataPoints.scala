// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1

import com.cognite.sdk.scala.common.{AggregateDataPoint, DataPoint, StringDataPoint}
import com.github.plokhotnyuk.jsoniter_scala.core.{
  JsonReader,
  JsonValueCodec,
  JsonWriter
}

final case class DataPointsByExternalId(
    externalId: String,
    datapoints: Seq[DataPoint]
)

trait DataPointsResponse[D] {
  def id: Long
  def getExternalId: Option[String]
  def isString: Boolean
  def unit: Option[String]
  def datapoints: Seq[D]
}

final case class DataPointsByIdResponse(
    id: Long,
    externalId: Option[String],
    isString: Boolean,
    isStep: Boolean = true, // default value for accidental string points read
    unit: Option[String],
    datapoints: Seq[DataPoint]
) extends DataPointsResponse[DataPoint] {
  override def getExternalId: Option[String] = externalId
}

final case class DataPointsByExternalIdResponse(
    id: Long,
    externalId: String,
    isString: Boolean,
    isStep: Boolean = true, // default value for accidental string points read
    unit: Option[String],
    datapoints: Seq[DataPoint]
) extends DataPointsResponse[DataPoint] {
  override def getExternalId: Option[String] = Some(externalId)
}

final case class StringDataPointsByIdResponse(
    id: Long,
    externalId: Option[String],
    isString: Boolean,
    unit: Option[String],
    datapoints: Seq[StringDataPoint]
) extends DataPointsResponse[StringDataPoint] {
  override def getExternalId: Option[String] = externalId
}

final case class StringDataPointsByExternalIdResponse(
    id: Long,
    externalId: String,
    isString: Boolean,
    unit: Option[String],
    datapoints: Seq[StringDataPoint]
) extends DataPointsResponse[StringDataPoint] {
  override def getExternalId: Option[String] = Some(externalId)
}

final case class StringDataPointsByExternalId(
    externalId: String,
    datapoints: Seq[StringDataPoint]
)

final case class DeleteDataPointsRange(
    id: CogniteId,
    inclusiveBegin: Long,
    exclusiveEnd: Long
)

object DeleteDataPointsRange {
  // scalastyle:off cyclomatic.complexity
  implicit val encoder: JsonValueCodec[DeleteDataPointsRange] =
    new JsonValueCodec[DeleteDataPointsRange] {
      override def decodeValue(
          in: JsonReader,
          default: DeleteDataPointsRange
      ): DeleteDataPointsRange =
        if (in.isNextToken('{')) {
          var inclusiveBegin = -1L
          var exclusiveEnd = -1L
          var id: Option[Long] = None
          var externalId: Option[String] = None
          if (!in.isNextToken('}')) {
            in.rollbackToken()
            var l = -1
            while (l < 0 || in.isNextToken(',')) {
              l = in.readKeyAsCharBuf()
              if (in.isCharBufEqualsTo(l, "inclusiveBegin")) {
                inclusiveBegin = in.readLong()
              } else if (in.isCharBufEqualsTo(l, "exclusiveEnd")) {
                exclusiveEnd = in.readLong()
              } else if (in.isCharBufEqualsTo(l, "id")) {
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
            if (inclusiveBegin < 0) {
              in.decodeError("'inclusiveBegin' missing")
            }
            if (exclusiveEnd < 0) {
              in.decodeError("'exclusiveEnd' missing")
            }
            (id, externalId) match {
              case (Some(internalId), None) =>
                DeleteDataPointsRange(CogniteInternalId(internalId), inclusiveBegin, exclusiveEnd)
              case (None, Some(externalId)) =>
                DeleteDataPointsRange(CogniteExternalId(externalId), inclusiveBegin, exclusiveEnd)
              case (Some(_), Some(_)) =>
                in.decodeError("Both 'id' and 'externalId' found")
            }
          } else {
            in.readNullOrTokenError(default, '{')
          }
        } else {
          in.readNullOrTokenError(default, '{')
        }

      override def encodeValue(x: DeleteDataPointsRange, out: JsonWriter): Unit = {
        out.writeObjectStart()
        x.id match {
          case CogniteExternalId(externalId) =>
            out.writeKey("externalId")
            out.writeVal(externalId)
          case CogniteInternalId(id) =>
            out.writeKey("id")
            out.writeVal(id)
        }
        out.writeKey("inclusiveBegin")
        out.writeVal(x.inclusiveBegin)
        out.writeKey("exclusiveEnd")
        out.writeVal(x.exclusiveEnd)
        out.writeObjectEnd()
      }

      override def nullValue: DeleteDataPointsRange = null // scalastyle:ignore null
    }
}

final case class LatestBeforeRequest(
    before: String,
    id: CogniteId
)
object LatestBeforeRequest {
  implicit val encoder: JsonValueCodec[LatestBeforeRequest] = Codec.instance(v =>
    Json.obj(
      "before" -> Json.fromString(v.before),
      v.id match {
        case CogniteExternalId(externalId) => "externalId" -> Json.fromString(externalId)
        case CogniteInternalId(id) => "id" -> Json.fromLong(id)
      }
    )
  )
}

final case class QueryDataPointsRange(
    id: CogniteId,
    start: String,
    end: String,
    limit: Option[Int] = None,
    granularity: Option[String] = None,
    aggregates: Option[Seq[String]] = None
)
object QueryDataPointsRange {
  implicit val encoder: JsonValueCodec[QueryDataPointsRange] = new JsonValueCodec[QueryDataPointsRange] {
    override def decodeValue(in: JsonReader, default: QueryDataPointsRange): QueryDataPointsRange = ???

    override def encodeValue(x: QueryDataPointsRange, out: JsonWriter): Unit = {
      out.writeObjectStart()
      x.id match {
        case CogniteExternalId(externalId) =>
          out.writeKey("externalId")
          out.writeVal(externalId)
        case CogniteInternalId(id) =>
          out.writeKey("id")
          out.writeVal(id)
      }
      out.writeKey("start")
      out.writeVal(x.start)
      out.writeKey("end")
      out.writeVal(x.end)
      x.limit.foreach { limit =>
        out.writeKey("limit")
        out.writeVal(limit)
      }
      x.granularity.foreach { granularity =>
        out.writeKey("granularity")
        out.writeVal(granularity)
      }
      x.aggregates.foreach { aggregates =>
        out.writeKey("aggregates")
        out.writeArrayStart()
        aggregates.foreach(out.writeVal)
        out.writeArrayEnd()
      }
      out.writeObjectEnd()
    }

    override def nullValue: QueryDataPointsRange = null
  }
}

final case class QueryAggregatesResponse(
    id: Long,
    externalId: Option[String],
    isString: Boolean,
    isStep: Boolean,
    unit: Option[String],
    datapoints: Seq[AggregateDataPoint]
)
