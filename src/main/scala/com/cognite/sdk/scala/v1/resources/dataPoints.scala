// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1.resources

import cats.implicits._
import com.cognite.sdk.scala.common._
import com.cognite.sdk.scala.v1._
import com.cognite.v1.timeseries.proto._
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.parser.decode
import io.circe.{Decoder, Encoder}
import sttp.client3._
import sttp.client3.circe._
import sttp.model.{MediaType, Uri}

import java.nio.charset.StandardCharsets
import java.time.Instant
import scala.collection.JavaConverters._ // Avoid scala.jdk to keep 2.12 compatibility without scala-collection-compat
import scala.util.control.NonFatal

class DataPointsResource[F[_]](val requestSession: RequestSession[F])
    extends WithRequestSession[F]
    with BaseUrl {
  import DataPointsResource._

  override val baseUrl = uri"${requestSession.baseUrl}/timeseries/data"

  private def protoNumericDataPoints(dataPoints: Seq[DataPoint]): NumericDatapoints.Builder =
    NumericDatapoints
      .newBuilder()
      .addAllDatapoints(dataPoints.map { dp =>
        NumericDatapoint
          .newBuilder()
          .setValue(dp.value)
          .setTimestamp(dp.timestamp.toEpochMilli)
          .build()
      }.asJava)

  private def protoStringDataPoints(dataPoints: Seq[StringDataPoint]): StringDatapoints.Builder =
    StringDatapoints
      .newBuilder()
      .addAllDatapoints(dataPoints.map { dp =>
        StringDatapoint
          .newBuilder()
          .setValue(dp.value)
          .setTimestamp(dp.timestamp.toEpochMilli)
          .build()
      }.asJava)

  private def arrayByteSerializer(a: Array[Byte]): ByteArrayBody = ByteArrayBody(a)

  def insert(id: CogniteId, dataPoints: Seq[DataPoint]): F[Unit] = {
    val byteArrayBody = DataPointInsertionRequest
      .newBuilder()
      .addItems {
        val b = DataPointInsertionItem.newBuilder()
        (id match {
          case CogniteInternalId(id) => b.setId(id)
          case CogniteExternalId(externalId) => b.setExternalId(externalId)
        }).setNumericDatapoints(protoNumericDataPoints(dataPoints))
          .build()
      }
      .build()
      .toByteArray

    requestSession.post[Unit, Unit, Array[Byte]](
      byteArrayBody,
      baseUrl,
      _ => (),
      "application/protobuf"
    )(arrayByteSerializer, implicitly)
  }

  def insertById(id: Long, dataPoints: Seq[DataPoint]): F[Unit] =
    insert(CogniteInternalId(id), dataPoints)

  def insertByExternalId(externalId: String, dataPoints: Seq[DataPoint]): F[Unit] =
    insert(CogniteExternalId(externalId), dataPoints)

  def insertStrings(id: CogniteId, dataPoints: Seq[StringDataPoint]): F[Unit] = {
    val byteArrayBody = DataPointInsertionRequest
      .newBuilder()
      .addItems {
        val b = DataPointInsertionItem.newBuilder()
        (id match {
          case CogniteInternalId(id) => b.setId(id)
          case CogniteExternalId(externalId) => b.setExternalId(externalId)
        }).setStringDatapoints(
          protoStringDataPoints(dataPoints)
            .build()
        )
      }
      .build()
      .toByteArray

    requestSession.post[Unit, Unit, Array[Byte]](
      byteArrayBody,
      baseUrl,
      _ => (),
      "application/protobuf"
    )(arrayByteSerializer, implicitly)
  }

  def insertStringsById(id: Long, dataPoints: Seq[StringDataPoint]): F[Unit] =
    insertStrings(CogniteInternalId(id), dataPoints)

  def insertStringsByExternalId(externalId: String, dataPoints: Seq[StringDataPoint]): F[Unit] =
    insertStrings(CogniteExternalId(externalId), dataPoints)

  def deleteRanges(ranges: Seq[DeleteDataPointsRange]): F[Unit] =
    requestSession.post[Unit, Unit, Items[DeleteDataPointsRange]](
      Items(ranges),
      uri"$baseUrl/delete",
      _ => ()
    )

  def deleteRange(id: CogniteId, inclusiveStart: Instant, exclusiveEnd: Instant): F[Unit] =
    deleteRanges(
      Seq(DeleteDataPointsRange(id, inclusiveStart.toEpochMilli, exclusiveEnd.toEpochMilli))
    )
  def deleteRangeById(id: Long, inclusiveStart: Instant, exclusiveEnd: Instant): F[Unit] =
    deleteRange(CogniteInternalId(id), inclusiveStart, exclusiveEnd)

  def deleteRangeByExternalId(
      externalId: String,
      inclusiveStart: Instant,
      exclusiveEnd: Instant
  ): F[Unit] =
    deleteRange(CogniteExternalId(externalId), inclusiveStart, exclusiveEnd)

  def queryById(
      id: Long,
      inclusiveStart: Instant,
      exclusiveEnd: Instant,
      limit: Option[Int] = None
  ): F[DataPointsByIdResponse] =
    queryOne(CogniteInternalId(id), inclusiveStart, exclusiveEnd, limit)

  def queryByIds(
      ids: Seq[Long],
      inclusiveStart: Instant,
      exclusiveEnd: Instant,
      limit: Option[Int] = None,
      ignoreUnknownIds: Boolean = false
  ): F[Seq[DataPointsByIdResponse]] =
    query(ids.map(CogniteInternalId(_)), inclusiveStart, exclusiveEnd, limit, ignoreUnknownIds)

  @SuppressWarnings(Array("org.wartremover.warts.IterableOps"))
  def queryOne(
      id: CogniteId,
      inclusiveStart: Instant,
      exclusiveEnd: Instant,
      limit: Option[Int] = None
  ): F[DataPointsByIdResponse] =
    query(Seq(id), inclusiveStart, exclusiveEnd, limit)
      .map(_.head)

  def query(
      ids: Seq[CogniteId],
      inclusiveStart: Instant,
      exclusiveEnd: Instant,
      limit: Option[Int] = None,
      ignoreUnknownIds: Boolean = false
  ): F[Seq[DataPointsByIdResponse]] = {
    val queries: Seq[QueryDataPointsRange] = ids.map { id =>
      QueryDataPointsRange(
        id,
        inclusiveStart.toEpochMilli.toString,
        exclusiveEnd.toEpochMilli.toString,
        Some(limit.getOrElse(Constants.dataPointsBatchSize))
      )
    }
    queryProtobuf(ItemsWithIgnoreUnknownIds(queries, ignoreUnknownIds))(
      parseNumericDataPoints
    )
  }

  @SuppressWarnings(Array("org.wartremover.warts.IterableOps"))
  def queryByExternalId(
      externalId: String,
      inclusiveStart: Instant,
      exclusiveEnd: Instant,
      limit: Option[Int] = None
  ): F[DataPointsByExternalIdResponse] =
    // The API returns an error causing an exception to be thrown if the item isn't found,
    // so .head is safe here.
    queryByExternalIds(Seq(externalId), inclusiveStart, exclusiveEnd, limit)
      .map(_.head)

  def queryByExternalIds(
      externalIds: Seq[String],
      inclusiveStart: Instant,
      exclusiveEnd: Instant,
      limit: Option[Int] = None,
      ignoreUnknownIds: Boolean = false
  ): F[Seq[DataPointsByExternalIdResponse]] = {
    val queries = externalIds.map(externalId =>
      QueryDataPointsRange(
        CogniteExternalId(externalId),
        inclusiveStart.toEpochMilli.toString,
        exclusiveEnd.toEpochMilli.toString,
        Some(limit.getOrElse(Constants.dataPointsBatchSize))
      )
    )
    queryProtobuf(ItemsWithIgnoreUnknownIds(queries, ignoreUnknownIds))(
      parseNumericDataPointsByExternalId
    )
  }
  def queryAggregates(
      ids: Seq[CogniteId],
      inclusiveStart: Instant,
      exclusiveEnd: Instant,
      granularity: String,
      aggregates: Seq[String],
      limit: Option[Int] = None,
      ignoreUnknownIds: Boolean = false
  ): F[Map[String, Seq[DataPointsByIdResponse]]] =
    queryProtobuf(
      ItemsWithIgnoreUnknownIds(
        ids.map(
          QueryDataPointsRange(
            _,
            inclusiveStart.toEpochMilli.toString,
            exclusiveEnd.toEpochMilli.toString,
            Some(limit.getOrElse(Constants.aggregatesBatchSize)),
            Some(granularity),
            Some(aggregates)
          )
        ),
        ignoreUnknownIds
      )
    ) { dataPointListResponse =>
      toAggregateMap(parseAggregateDataPoints(dataPointListResponse))
    }

  def queryAggregatesById(
      id: Long,
      inclusiveStart: Instant,
      exclusiveEnd: Instant,
      granularity: String,
      aggregates: Seq[String],
      limit: Option[Int] = None
  ): F[Map[String, Seq[DataPointsByIdResponse]]] =
    queryAggregates(
      Seq(CogniteInternalId(id)),
      inclusiveStart,
      exclusiveEnd,
      granularity,
      aggregates,
      limit
    )

  def queryAggregatesByExternalId(
      externalId: String,
      inclusiveStart: Instant,
      exclusiveEnd: Instant,
      granularity: String,
      aggregates: Seq[String],
      limit: Option[Int] = None
  ): F[Map[String, Seq[DataPointsByIdResponse]]] =
    queryAggregates(
      Seq(CogniteExternalId(externalId)),
      inclusiveStart,
      exclusiveEnd,
      granularity,
      aggregates,
      limit
    )

  private def queryProtobuf[Q: Encoder, R](
      query: Q
  )(mapDataPointList: DataPointListResponse => R): F[R] =
    requestSession
      .sendCdf(
        request =>
          request
            .post(uri"$baseUrl/list")
            .body(query)
            .response(
              asProtobufOrError(uri"$baseUrl/list")
                .mapRight(mapDataPointList)
                .getRight
            ),
        accept = "application/protobuf"
      )

  @SuppressWarnings(Array("org.wartremover.warts.IterableOps"))
  def queryStringsById(
      id: Long,
      inclusiveStart: Instant,
      exclusiveEnd: Instant,
      limit: Option[Int] = None
  ): F[StringDataPointsByIdResponse] =
    // The API returns an error causing an exception to be thrown if the item isn't found,
    // so .head is safe here.
    queryStrings(Seq(CogniteInternalId(id)), inclusiveStart, exclusiveEnd, limit)
      .map(_.head)

  @SuppressWarnings(Array("org.wartremover.warts.IterableOps"))
  def queryStringsByExternalId(
      externalId: String,
      inclusiveStart: Instant,
      exclusiveEnd: Instant,
      limit: Option[Int] = None
  ): F[StringDataPointsByExternalIdResponse] =
    // The API returns an error causing an exception to be thrown if the item isn't found,
    // so .head is safe here.
    queryStringsByExternalIds(Seq(externalId), inclusiveStart, exclusiveEnd, limit)
      .map(_.head)

  def queryStrings(
      ids: Seq[CogniteId],
      inclusiveStart: Instant,
      exclusiveEnd: Instant,
      limit: Option[Int] = None,
      ignoreUnknownIds: Boolean = false
  ): F[Seq[StringDataPointsByIdResponse]] = {
    val query = ids.map(id =>
      QueryDataPointsRange(
        id,
        inclusiveStart.toEpochMilli.toString,
        exclusiveEnd.toEpochMilli.toString,
        Some(limit.getOrElse(Constants.dataPointsBatchSize))
      )
    )
    queryProtobuf(ItemsWithIgnoreUnknownIds(query, ignoreUnknownIds))(parseStringDataPoints)
  }

  def queryStringsByExternalIds(
      externalIds: Seq[String],
      inclusiveStart: Instant,
      exclusiveEnd: Instant,
      limit: Option[Int] = None,
      ignoreUnknownIds: Boolean = false
  ): F[Seq[StringDataPointsByExternalIdResponse]] = {
    val query =
      externalIds.map(id =>
        QueryDataPointsRange(
          CogniteExternalId(id),
          inclusiveStart.toEpochMilli.toString,
          exclusiveEnd.toEpochMilli.toString,
          Some(limit.getOrElse(Constants.dataPointsBatchSize))
        )
      )
    queryProtobuf(ItemsWithIgnoreUnknownIds(query, ignoreUnknownIds))(
      parseStringDataPointsByExternalId
    )
  }

  def getLatestDataPoint(id: CogniteId, before: String = "now"): F[Option[DataPoint]] =
    getLatestDataPoints(Seq(id), before = before)
      .flatMap(
        _.get(id) match {
          case Some(latest) => FMonad.pure(latest)
          case None =>
            FMonad.raiseError(
              SdkException(
                s"Unexpected missing ${id.toString} when retrieving latest data point"
              )
            )
        }
      )

  def getLatestDataPoints(
      ids: Seq[CogniteId],
      ignoreUnknownIds: Boolean = false,
      before: String = "now"
  ): F[Map[CogniteId, Option[DataPoint]]] =
    getLatestDataPointsCommon[DataPoint, DataPointsByIdResponse](ids, ignoreUnknownIds, before)

  def getLatestStringDataPoint(
      id: CogniteId,
      before: String = "now"
  ): F[Option[StringDataPoint]] =
    getLatestStringDataPoints(Seq(id), before = before)
      .flatMap(_.get(id) match {
        case Some(latest) => FMonad.pure(latest)
        case None =>
          FMonad.raiseError(
            SdkException(
              s"Unexpected missing ${id.toString} when retrieving latest data point"
            )
          )
      })

  def getLatestStringDataPoints(
      ids: Seq[CogniteId],
      ignoreUnknownIds: Boolean = false,
      before: String = "now"
  ): F[Map[CogniteId, Option[StringDataPoint]]] =
    getLatestDataPointsCommon[StringDataPoint, StringDataPointsByIdResponse](
      ids,
      ignoreUnknownIds,
      before
    )

  private def getLatestDataPointsCommon[D, T <: DataPointsResponse[D]](
      ids: Seq[CogniteId],
      ignoreUnknownIds: Boolean,
      /* Get datapoints before this time. The format is N[timeunit]-ago where timeunit is w,d,h,m,s.
      Example: '2d-ago' gets data that is up to 2 days old. You can also specify time in milliseconds since epoch. */
      before: String
  )(implicit decoder: Decoder[Items[T]]): F[Map[CogniteId, Option[D]]] =
    requestSession
      .post[Map[CogniteId, Option[D]], Items[T], ItemsWithIgnoreUnknownIds[LatestBeforeRequest]](
        ItemsWithIgnoreUnknownIds(
          ids.map(id => LatestBeforeRequest(before, id)),
          ignoreUnknownIds
        ),
        uri"$baseUrl/latest",
        value => {
          val idMap = value.items.map(item => item.id -> item.datapoints.headOption).toMap
          val externalIdMap =
            value.items.map(item => item.getExternalId -> item.datapoints.headOption).toMap
          ids
            .map { i =>
              i -> (i match {
                case CogniteInternalId(id) => idMap.get(id)
                case CogniteExternalId(externalId) => externalIdMap.get(Some(externalId))
              })
            }
            .collect { case (id, Some(v)) =>
              id -> v
            }
            .toMap
        }
      )
}

object DataPointsResource {
  implicit val dataPointDecoder: Decoder[DataPoint] = deriveDecoder
  implicit val dataPointEncoder: Encoder[DataPoint] = deriveEncoder
  implicit val dataPointsByIdResponseDecoder: Decoder[DataPointsByIdResponse] = deriveDecoder
  implicit val dataPointsByIdResponseItemsDecoder: Decoder[Items[DataPointsByIdResponse]] =
    deriveDecoder
  implicit val dataPointsByExternalIdResponseDecoder: Decoder[DataPointsByExternalIdResponse] =
    deriveDecoder
  implicit val dataPointsByExternalIdResponseItemsDecoder
      : Decoder[Items[DataPointsByExternalIdResponse]] = deriveDecoder

  // WartRemover gets confused by circe-derivation
  @SuppressWarnings(Array("org.wartremover.warts.JavaSerializable"))
  implicit val stringDataPointDecoder: Decoder[StringDataPoint] = deriveDecoder
  implicit val stringDataPointEncoder: Encoder[StringDataPoint] = deriveEncoder
  implicit val stringDataPointsByIdResponseDecoder: Decoder[StringDataPointsByIdResponse] =
    deriveDecoder
  implicit val stringDataPointsByIdResponseItemsDecoder
      : Decoder[Items[StringDataPointsByIdResponse]] = deriveDecoder
  implicit val stringDataPointsByExternalIdResponseDecoder
      : Decoder[StringDataPointsByExternalIdResponse] = deriveDecoder
  implicit val stringDataPointsByExternalIdResponseItemsDecoder
      : Decoder[Items[StringDataPointsByExternalIdResponse]] = deriveDecoder
  implicit val dataPointsByExternalIdEncoder: Encoder[DataPointsByExternalId] = deriveEncoder
  implicit val dataPointsByExternalIdItemsEncoder: Encoder[Items[DataPointsByExternalId]] =
    deriveEncoder
  implicit val stringDataPointsByExternalIdEncoder: Encoder[StringDataPointsByExternalId] =
    deriveEncoder
  implicit val stringDataPointsByExternalIdItemsEncoder
      : Encoder[Items[StringDataPointsByExternalId]] = deriveEncoder
  implicit val deleteRangeItemsEncoder: Encoder[Items[DeleteDataPointsRange]] = deriveEncoder
  implicit val queryRangeByIdItemsEncoder: Encoder[Items[QueryDataPointsRange]] = deriveEncoder
  implicit val queryRangeByIdItems2Encoder
      : Encoder[ItemsWithIgnoreUnknownIds[QueryDataPointsRange]] =
    deriveEncoder

  implicit val queryLatestByIdItems2Encoder
      : Encoder[ItemsWithIgnoreUnknownIds[LatestBeforeRequest]] = deriveEncoder
  @SuppressWarnings(Array("org.wartremover.warts.JavaSerializable"))
  implicit val aggregateDataPointDecoder: Decoder[AggregateDataPoint] = deriveDecoder
  implicit val aggregateDataPointEncoder: Encoder[AggregateDataPoint] = deriveEncoder

  // protobuf can't represent `null`, so we'll assume that empty string is null...
  private def optionalString(x: String) =
    if (x.isEmpty) {
      None
    } else {
      Some(x)
    }

  def parseStringDataPoints(response: DataPointListResponse): Seq[StringDataPointsByIdResponse] =
    response.getItemsList.asScala
      .map(x =>
        StringDataPointsByIdResponse(
          x.getId,
          optionalString(x.getExternalId),
          x.getIsString,
          optionalString(x.getUnit),
          x.getStringDatapoints.getDatapointsList.asScala
            .map(s => StringDataPoint(Instant.ofEpochMilli(s.getTimestamp), s.getValue))
            .toSeq // Required by Scala 2.13 and later, to make this immutable
        )
      )
      .toSeq

  def parseStringDataPointsByExternalId(
      response: DataPointListResponse
  ): Seq[StringDataPointsByExternalIdResponse] =
    response.getItemsList.asScala
      .map(x =>
        StringDataPointsByExternalIdResponse(
          x.getId,
          x.getExternalId,
          x.getIsString,
          optionalString(x.getUnit),
          x.getStringDatapoints.getDatapointsList.asScala
            .map(s => StringDataPoint(Instant.ofEpochMilli(s.getTimestamp), s.getValue))
            .toSeq // Required by Scala 2.13 and later, to make this immutable
        )
      )
      .toSeq

  def parseNumericDataPoints(response: DataPointListResponse): Seq[DataPointsByIdResponse] =
    response.getItemsList.asScala.map { x =>
      DataPointsByIdResponse(
        x.getId,
        optionalString(x.getExternalId),
        x.getIsString,
        x.getIsStep,
        optionalString(x.getUnit),
        x.getNumericDatapoints.getDatapointsList.asScala
          .map(n => DataPoint(Instant.ofEpochMilli(n.getTimestamp), n.getValue))
          .toSeq // Required by Scala 2.13 and later, to make this immutable
      )
    }.toSeq

  def parseNumericDataPointsByExternalId(
      response: DataPointListResponse
  ): Seq[DataPointsByExternalIdResponse] =
    response.getItemsList.asScala.map { x =>
      DataPointsByExternalIdResponse(
        x.getId,
        x.getExternalId,
        x.getIsString,
        x.getIsStep,
        optionalString(x.getUnit),
        x.getNumericDatapoints.getDatapointsList.asScala
          .map(n => DataPoint(Instant.ofEpochMilli(n.getTimestamp), n.getValue))
          .toSeq // Required by Scala 2.13 and later, to make this immutable
      )
    }.toSeq

  def screenOutNan(d: Double): Option[Double] =
    if (d.isNaN) None else Some(d)

  def parseAggregateDataPoints(response: DataPointListResponse): Seq[QueryAggregatesResponse] =
    response.getItemsList.asScala
      .map(x =>
        QueryAggregatesResponse(
          x.getId,
          Some(x.getExternalId),
          x.getIsString,
          x.getIsStep,
          Some(x.getUnit),
          x.getAggregateDatapoints.getDatapointsList.asScala
            .map(a =>
              AggregateDataPoint(
                Instant.ofEpochMilli(a.getTimestamp),
                screenOutNan(a.getAverage),
                screenOutNan(a.getMax),
                screenOutNan(a.getMin),
                screenOutNan(a.getCount),
                screenOutNan(a.getSum),
                screenOutNan(a.getInterpolation),
                screenOutNan(a.getStepInterpolation),
                screenOutNan(a.getTotalVariation),
                screenOutNan(a.getContinuousVariance),
                screenOutNan(a.getDiscreteVariance)
              )
            )
            .toSeq // Required by Scala 2.13 and later, to make this immutable
        )
      )
      .toSeq

  private def asJsonOrSdkException(uri: Uri) = asJsonAlways[CdpApiError].mapWithMetadata {
    (response, metadata) =>
      response match {
        case Left(error) =>
          throw SdkException(
            s"Failed to parse response, reason: ${error.getMessage}",
            Some(uri),
            metadata.header("x-request-id"),
            Some(metadata.code.code)
          )
        case Right(cdpApiError) =>
          throw cdpApiError.asException(uri"$uri", metadata.header("x-request-id"))
      }
  }

  private def asProtoBuf(uri: Uri) = asByteArray.mapWithMetadata { (response, metadata) =>
    // TODO: Can use the HTTP headers in .mapWithMetaData to choose to parse as json or protobuf
    response match {
      case Left(_) =>
        val message = if (metadata.statusText.isEmpty) {
          "Unknown error (no status text)"
        } else {
          metadata.statusText
        }
        throw SdkException(
          message,
          Some(uri),
          metadata.header("x-request-id"),
          Some(metadata.code.code)
        )
      case Right(bytes) =>
        try DataPointListResponse.parseFrom(bytes)
        catch {
          case NonFatal(_) =>
            val s = new String(bytes, StandardCharsets.UTF_8)
            val shouldParse = metadata.contentLength.exists(_ > 0) &&
              metadata.contentType.exists(_.startsWith(MediaType.ApplicationJson.toString))
            if (shouldParse) {
              decode[CdpApiError](s) match {
                case Left(error) => throw error
                case Right(cdpApiError) =>
                  throw cdpApiError.asException(uri, metadata.header("x-request-id"))
              }
            } else {
              val message = if (metadata.statusText.isEmpty) {
                "Unknown error (no status text)"
              } else {
                metadata.statusText
              }
              throw SdkException(
                message,
                Some(uri),
                metadata.header("x-request-id"),
                Some(metadata.code.code)
              )
            }
        }
    }
  }

  private def asProtobufOrError(
      uri: Uri
  ): ResponseAs[Either[CdpApiError, DataPointListResponse], Any] =
    asEither(asJsonOrSdkException(uri), asProtoBuf(uri))

  private def toAggregateMap(
      aggregateDataPoints: Seq[QueryAggregatesResponse]
  ): Map[String, Seq[DataPointsByIdResponse]] =
    Map(
      "average" -> extractAggregates(aggregateDataPoints, _.average),
      "max" -> extractAggregates(aggregateDataPoints, _.max),
      "min" -> extractAggregates(aggregateDataPoints, _.min),
      "count" -> extractAggregates(aggregateDataPoints, _.count),
      "sum" -> extractAggregates(aggregateDataPoints, _.sum),
      "interpolation" -> extractAggregates(aggregateDataPoints, _.interpolation),
      "stepInterpolation" -> extractAggregates(aggregateDataPoints, _.stepInterpolation),
      "continuousVariance" -> extractAggregates(aggregateDataPoints, _.continuousVariance),
      "discreteVariance" -> extractAggregates(aggregateDataPoints, _.discreteVariance),
      "totalVariation" -> extractAggregates(aggregateDataPoints, _.totalVariation)
    ).filter(kv => kv._2.nonEmpty)
  private def extractAggregates(
      adp: Seq[QueryAggregatesResponse],
      f: AggregateDataPoint => Option[Double]
  ): Seq[DataPointsByIdResponse] =
    adp
      .map(r =>
        DataPointsByIdResponse(
          r.id,
          r.externalId,
          r.isString,
          r.isStep,
          r.unit,
          r.datapoints.flatMap(dp => f(dp).toList.map(v => DataPoint(dp.timestamp, v)))
        )
      )
      .filter(_.datapoints.nonEmpty)
}
