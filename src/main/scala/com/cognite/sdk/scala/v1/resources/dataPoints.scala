// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1.resources

import java.nio.charset.StandardCharsets
import java.time.Instant
import BuildInfo.BuildInfo
import com.cognite.sdk.scala.common._
import com.cognite.sdk.scala.v1._
import sttp.client3._
import sttp.client3.circe._
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import io.circe.syntax._
import com.cognite.sdk.scala.common.Constants
import com.cognite.v1.timeseries.proto._
import io.circe.parser.decode
import sttp.model.{MediaType, Uri}

import scala.collection.JavaConverters._
import scala.concurrent.duration._
import scala.util.control.NonFatal

class DataPointsResource[F[_]](val requestSession: RequestSession[F])
    extends WithRequestSession[F]
    with BaseUrl {

  import DataPointsResource._

  override val baseUrl = uri"${requestSession.baseUrl}/timeseries/data"

  private def protoNumericDataPoints(dataPoints: Seq[DataPoint]) =
    NumericDatapoints
      .newBuilder()
      .addAllDatapoints(dataPoints.map { dp =>
        NumericDatapoint
          .newBuilder()
          .setValue(dp.value)
          .setTimestamp(dp.timestamp.toEpochMilli)
          .build()
      }.asJava)

  private def protoStringDataPoints(dataPoints: Seq[StringDataPoint]) =
    StringDatapoints
      .newBuilder()
      .addAllDatapoints(dataPoints.map { dp =>
        StringDatapoint
          .newBuilder()
          .setValue(dp.value)
          .setTimestamp(dp.timestamp.toEpochMilli)
          .build()
      }.asJava)

  // FIXME: Using requestSession.post() resulted in 500 errors when the request involves protobuf because of some
  //  serialization error. Should investigate the error and use requestSession.post rather than having its own
  //  identical handling for this method and insertStringsById()
  def insertById(id: Long, dataPoints: Seq[DataPoint]): F[Unit] =
    requestSession.map(
      basicRequest
        .followRedirects(false)
        .contentType("application/protobuf")
        .header("accept", "application/json")
        .header("x-cdp-sdk", s"${BuildInfo.organization}-${BuildInfo.version}")
        .header("x-cdp-app", requestSession.applicationName)
        .readTimeout(90.seconds)
        .post(baseUrl)
        .body(
          DataPointInsertionRequest
            .newBuilder()
            .addItems(
              DataPointInsertionItem
                .newBuilder()
                .setId(id)
                .setNumericDatapoints(protoNumericDataPoints(dataPoints))
                .build()
            )
            .build()
            .toByteArray
        )
        .response(
          asJsonEither[CdpApiError, Unit].mapWithMetadata((response, metadata) =>
            response match {
              case Left(value) =>
                value match {
                  case HttpError(cdpApiError, _) =>
                    throw cdpApiError.asException(baseUrl, metadata.header("x-request-id"))
                  case DeserializationException(_, error) =>
                    throw SdkException(
                      s"Failed to parse response, reason: ${error.getMessage}",
                      Some(baseUrl),
                      metadata.header("x-request-id"),
                      Some(metadata.code.code)
                    )
                }
              case Right(_) => ()
            }
          )
        )
        .send(requestSession.sttpBackend),
      (r: Response[Unit]) => r.body
    )

  def insertByExternalId(externalId: String, dataPoints: Seq[DataPoint]): F[Unit] =
    requestSession.post[Unit, Unit, Items[DataPointsByExternalId]](
      Items(Seq(DataPointsByExternalId(externalId, dataPoints))),
      baseUrl,
      _ => ()
    )

  def insertStringsById(id: Long, dataPoints: Seq[StringDataPoint]): F[Unit] =
    requestSession.map(
      basicRequest
        .followRedirects(false)
        .contentType("application/protobuf")
        .header("accept", "application/json")
        .header("x-cdp-sdk", s"${BuildInfo.organization}-${BuildInfo.version}")
        .header("x-cdp-app", requestSession.applicationName)
        .post(baseUrl)
        .body(
          DataPointInsertionRequest
            .newBuilder()
            .addItems(
              DataPointInsertionItem
                .newBuilder()
                .setId(id)
                .setStringDatapoints(
                  protoStringDataPoints(dataPoints)
                    .build()
                )
            )
            .build()
            .toByteArray
        )
        .response(
          asJsonEither[CdpApiError, Unit].mapWithMetadata((response, metadata) =>
            response match {
              case Left(value) =>
                value match {
                  case HttpError(cdpApiError, _) =>
                    throw cdpApiError.asException(baseUrl, metadata.header("x-request-id"))
                  case DeserializationException(_, error) =>
                    throw SdkException(
                      s"Failed to parse response, reason: ${error.getMessage}",
                      Some(baseUrl),
                      metadata.header("x-request-id"),
                      Some(metadata.code.code)
                    )
                }
              case Right(_) => ()
            }
          )
        )
        .send(requestSession.sttpBackend),
      (r: Response[Unit]) => r.body
    )

  def insertStringsByExternalId(externalId: String, dataPoints: Seq[StringDataPoint]): F[Unit] =
    requestSession.post[Unit, Unit, Items[StringDataPointsByExternalId]](
      Items(Seq(StringDataPointsByExternalId(externalId, dataPoints))),
      baseUrl,
      _ => ()
    )

  def deleteRanges(ranges: Seq[DeleteDataPointsRange]): F[Unit] =
    requestSession.post[Unit, Unit, Items[DeleteDataPointsRange]](
      Items(ranges),
      uri"$baseUrl/delete",
      _ => ()
    )

  def deleteRangeById(id: Long, inclusiveStart: Instant, exclusiveEnd: Instant): F[Unit] =
    deleteRanges(Seq(DeleteRangeById(id, inclusiveStart.toEpochMilli, exclusiveEnd.toEpochMilli)))

  def deleteRangeByExternalId(
      externalId: String,
      inclusiveStart: Instant,
      exclusiveEnd: Instant
  ): F[Unit] =
    deleteRanges(
      Seq(
        DeleteRangeByExternalId(
          externalId,
          inclusiveStart.toEpochMilli,
          exclusiveEnd.toEpochMilli
        )
      )
    )

  @SuppressWarnings(Array("org.wartremover.warts.TraversableOps"))
  def queryById(
      id: Long,
      inclusiveStart: Instant,
      exclusiveEnd: Instant,
      limit: Option[Int] = None
  ): F[DataPointsByIdResponse] =
    // The API returns an error causing an exception to be thrown if the item isn't found,
    // so .head is safe here.
    requestSession.map(
      queryByIds(Seq(id), inclusiveStart, exclusiveEnd, limit),
      (r1: Seq[DataPointsByIdResponse]) => r1.head
    )

  def queryByIds(
      ids: Seq[Long],
      inclusiveStart: Instant,
      exclusiveEnd: Instant,
      limit: Option[Int] = None,
      ignoreUnknownIds: Boolean = false
  ): F[Seq[DataPointsByIdResponse]] = {
    val queries = ids.map(id =>
      QueryRangeById(
        id,
        inclusiveStart.toEpochMilli.toString,
        exclusiveEnd.toEpochMilli.toString,
        Some(limit.getOrElse(Constants.dataPointsBatchSize))
      )
    )

    queryProtobuf(ItemsWithIgnoreUnknownIds(queries, ignoreUnknownIds))(
      parseNumericDataPoints
    )
  }

  @SuppressWarnings(Array("org.wartremover.warts.TraversableOps"))
  def queryByExternalId(
      externalId: String,
      inclusiveStart: Instant,
      exclusiveEnd: Instant,
      limit: Option[Int] = None
  ): F[DataPointsByExternalIdResponse] =
    // The API returns an error causing an exception to be thrown if the item isn't found,
    // so .head is safe here.
    requestSession.map(
      queryByExternalIds(Seq(externalId), inclusiveStart, exclusiveEnd, limit),
      (r1: Seq[DataPointsByExternalIdResponse]) => r1.head
    )

  def queryByExternalIds(
      externalIds: Seq[String],
      inclusiveStart: Instant,
      exclusiveEnd: Instant,
      limit: Option[Int] = None,
      ignoreUnknownIds: Boolean = false
  ): F[Seq[DataPointsByExternalIdResponse]] = {
    val queries = externalIds.map(externalId =>
      QueryRangeByExternalId(
        externalId,
        inclusiveStart.toEpochMilli.toString,
        exclusiveEnd.toEpochMilli.toString,
        Some(limit.getOrElse(Constants.dataPointsBatchSize))
      )
    )
    queryProtobuf(ItemsWithIgnoreUnknownIds(queries, ignoreUnknownIds))(
      parseNumericDataPointsByExternalId
    )
  }
  def queryAggregatesByIds(
      ids: Seq[Long],
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
          QueryRangeById(
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

  def queryAggregatesByExternalIds(
      externalIds: Seq[String],
      inclusiveStart: Instant,
      exclusiveEnd: Instant,
      granularity: String,
      aggregates: Seq[String],
      limit: Option[Int] = None,
      ignoreUnknownIds: Boolean = false
  ): F[Map[String, Seq[DataPointsByIdResponse]]] =
    queryProtobuf(
      ItemsWithIgnoreUnknownIds(
        externalIds.map(
          QueryRangeByExternalId(
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
    queryAggregatesByIds(Seq(id), inclusiveStart, exclusiveEnd, granularity, aggregates, limit)

  def queryAggregatesByExternalId(
      externalId: String,
      inclusiveStart: Instant,
      exclusiveEnd: Instant,
      granularity: String,
      aggregates: Seq[String],
      limit: Option[Int] = None
  ): F[Map[String, Seq[DataPointsByIdResponse]]] =
    queryAggregatesByExternalIds(
      Seq(externalId),
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

  @SuppressWarnings(Array("org.wartremover.warts.TraversableOps"))
  def queryStringsById(
      id: Long,
      inclusiveStart: Instant,
      exclusiveEnd: Instant,
      limit: Option[Int] = None
  ): F[StringDataPointsByIdResponse] =
    // The API returns an error causing an exception to be thrown if the item isn't found,
    // so .head is safe here.
    requestSession.map(
      queryStringsByIds(Seq(id), inclusiveStart, exclusiveEnd, limit),
      (r: Seq[StringDataPointsByIdResponse]) => r.head
    )

  @SuppressWarnings(Array("org.wartremover.warts.TraversableOps"))
  def queryStringsByExternalId(
      externalId: String,
      inclusiveStart: Instant,
      exclusiveEnd: Instant,
      limit: Option[Int] = None
  ): F[StringDataPointsByExternalIdResponse] =
    // The API returns an error causing an exception to be thrown if the item isn't found,
    // so .head is safe here.
    requestSession.map(
      queryStringsByExternalIds(Seq(externalId), inclusiveStart, exclusiveEnd, limit),
      (r: Seq[StringDataPointsByExternalIdResponse]) => r.head
    )

  def queryStringsByIds(
      ids: Seq[Long],
      inclusiveStart: Instant,
      exclusiveEnd: Instant,
      limit: Option[Int] = None,
      ignoreUnknownIds: Boolean = false
  ): F[Seq[StringDataPointsByIdResponse]] = {
    val query = ids.map(id =>
      QueryRangeById(
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
        QueryRangeByExternalId(
          id,
          inclusiveStart.toEpochMilli.toString,
          exclusiveEnd.toEpochMilli.toString,
          Some(limit.getOrElse(Constants.dataPointsBatchSize))
        )
      )
    queryProtobuf(ItemsWithIgnoreUnknownIds(query, ignoreUnknownIds))(
      parseStringDataPointsByExternalId
    )
  }

  def getLatestDataPointById(id: Long): F[Option[DataPoint]] =
    requestSession.map(
      getLatestDataPointsByIds(Seq(id)),
      (idToLatest: Map[Long, Option[DataPoint]]) =>
        idToLatest.get(id) match {
          case Some(latest) => latest
          case None =>
            throw SdkException(
              s"Unexpected missing id ${id.toString} when retrieving latest data point"
            )
        }
    )

  def getLatestDataPointByExternalId(externalId: String): F[Option[DataPoint]] =
    requestSession.map(
      getLatestDataPointsByExternalIds(Seq(externalId)),
      (idToLatest: Map[String, Option[DataPoint]]) =>
        idToLatest.get(externalId) match {
          case Some(latest) => latest
          case None =>
            throw SdkException(
              s"Unexpected missing external id ${externalId.toString} when retrieving latest data point"
            )
        }
    )

  def getLatestDataPointsByIds(
      ids: Seq[Long],
      ignoreUnknownIds: Boolean = false
  ): F[Map[Long, Option[DataPoint]]] =
    requestSession
      .post[Map[Long, Option[DataPoint]], Items[DataPointsByIdResponse], ItemsWithIgnoreUnknownIds[
        CogniteId
      ]](
        ItemsWithIgnoreUnknownIds(ids.map(CogniteInternalId.apply), ignoreUnknownIds),
        uri"$baseUrl/latest",
        value =>
          value.items.map { item =>
            item.id -> item.datapoints.headOption
          }.toMap
      )

  def getLatestDataPointsByExternalIds(
      ids: Seq[String],
      ignoreUnknownIds: Boolean = false
  ): F[Map[String, Option[DataPoint]]] =
    requestSession
      .post[Map[String, Option[DataPoint]], Items[
        DataPointsByExternalIdResponse
      ], ItemsWithIgnoreUnknownIds[
        CogniteId
      ]](
        ItemsWithIgnoreUnknownIds(ids.map(CogniteExternalId.apply), ignoreUnknownIds),
        uri"$baseUrl/latest",
        value =>
          value.items.map { item =>
            item.externalId -> item.datapoints.headOption
          }.toMap
      )

  def getLatestStringDataPointById(id: Long): F[Option[StringDataPoint]] =
    requestSession.map(
      getLatestStringDataPointByIds(Seq(id)),
      (idToLatest: Map[Long, Option[StringDataPoint]]) =>
        idToLatest.get(id) match {
          case Some(latest) => latest
          case None =>
            throw SdkException(
              s"Unexpected missing id ${id.toString} when retrieving latest data point"
            )
        }
    )

  def getLatestStringDataPointByExternalId(externalId: String): F[Option[StringDataPoint]] =
    requestSession.map(
      getLatestStringDataPointByExternalIds(Seq(externalId)),
      (idToLatest: Map[String, Option[StringDataPoint]]) =>
        idToLatest.get(externalId) match {
          case Some(latest) => latest
          case None =>
            throw SdkException(
              s"Unexpected missing id ${externalId.toString} when retrieving latest data point"
            )
        }
    )

  def getLatestStringDataPointByIds(
      ids: Seq[Long],
      ignoreUnknownIds: Boolean = false
  ): F[Map[Long, Option[StringDataPoint]]] =
    requestSession
      .post[Map[Long, Option[StringDataPoint]], Items[
        StringDataPointsByIdResponse
      ], ItemsWithIgnoreUnknownIds[
        CogniteId
      ]](
        ItemsWithIgnoreUnknownIds(ids.map(CogniteInternalId.apply), ignoreUnknownIds),
        uri"$baseUrl/latest",
        value =>
          value.items.map { item =>
            item.id -> item.datapoints.headOption
          }.toMap
      )

  def getLatestStringDataPointByExternalIds(
      ids: Seq[String],
      ignoreUnknownIds: Boolean = false
  ): F[Map[String, Option[StringDataPoint]]] =
    requestSession
      .post[Map[String, Option[StringDataPoint]], Items[
        StringDataPointsByExternalIdResponse
      ], ItemsWithIgnoreUnknownIds[
        CogniteId
      ]](
        ItemsWithIgnoreUnknownIds(ids.map(CogniteExternalId.apply), ignoreUnknownIds),
        uri"$baseUrl/latest",
        value =>
          value.items.map { item =>
            item.externalId -> item.datapoints.headOption
          }.toMap
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
  implicit val deleteRangeByIdEncoder: Encoder[DeleteRangeById] = deriveEncoder
  implicit val deleteRangeByIdItemsEncoder: Encoder[Items[DeleteRangeById]] = deriveEncoder
  implicit val deleteRangeEncoder: Encoder[DeleteDataPointsRange] = Encoder.instance {
    case byId @ DeleteRangeById(_, _, _) => byId.asJson
    case byExternalId @ DeleteRangeByExternalId(_, _, _) => byExternalId.asJson
  }
  implicit val deleteRangeItemsEncoder: Encoder[Items[DeleteDataPointsRange]] = deriveEncoder
  implicit val deleteRangeByExternalIdEncoder: Encoder[DeleteRangeByExternalId] = deriveEncoder
  implicit val deleteRangeByExternalIdItemsEncoder: Encoder[Items[DeleteRangeByExternalId]] =
    deriveEncoder
  implicit val queryRangeByIdEncoder: Encoder[QueryRangeById] = deriveEncoder
  implicit val queryRangeByIdItemsEncoder: Encoder[Items[QueryRangeById]] = deriveEncoder
  implicit val queryRangeByIdItems2Encoder: Encoder[ItemsWithIgnoreUnknownIds[QueryRangeById]] =
    deriveEncoder
  implicit val queryRangeByExternalIdEncoder: Encoder[QueryRangeByExternalId] = deriveEncoder
  implicit val queryRangeByExternalIdItemsEncoder: Encoder[Items[QueryRangeByExternalId]] =
    deriveEncoder
  implicit val queryRangeByExternalIdItems2Encoder
      : Encoder[ItemsWithIgnoreUnknownIds[QueryRangeByExternalId]] = deriveEncoder
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
            .toSeq
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
            .toSeq
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
          .toSeq
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
          .toSeq
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
            .toSeq
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
              metadata.contentType.exists(_.startsWith(MediaType.ApplicationJson.toString()))
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
