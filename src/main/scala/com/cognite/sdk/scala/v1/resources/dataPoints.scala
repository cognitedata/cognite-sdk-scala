package com.cognite.sdk.scala.v1.resources

import java.nio.charset.StandardCharsets
import java.time.Instant

import cats.Show
import com.cognite.sdk.scala.common._
import com.cognite.sdk.scala.v1._
import com.softwaremill.sttp._
import com.softwaremill.sttp.circe._
import io.circe.derivation.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import com.cognite.v1.timeseries.proto.data_point_insertion_request.{
  DataPointInsertionItem,
  DataPointInsertionRequest
}
import com.cognite.v1.timeseries.proto.data_point_list_response.DataPointListResponse
import com.cognite.v1.timeseries.proto.data_points.{
  NumericDatapoint,
  NumericDatapoints,
  StringDatapoint,
  StringDatapoints
}
import com.cognite.sdk.scala.common.Constants
import io.circe.parser.decode

class DataPointsResource[F[_]](val requestSession: RequestSession[F])
    extends WithRequestSession[F]
    with BaseUri {

  import DataPointsResource._

  override val baseUri = uri"${requestSession.baseUri}/timeseries/data"

  implicit val errorOrDataPointsByIdResponseDecoder
      : Decoder[Either[CdpApiError, Items[DataPointsByIdResponse]]] =
    EitherDecoder.eitherDecoder[CdpApiError, Items[DataPointsByIdResponse]]
  implicit val errorOrDataPointsByExternalIdResponseDecoder
      : Decoder[Either[CdpApiError, Items[DataPointsByExternalIdResponse]]] =
    EitherDecoder.eitherDecoder[CdpApiError, Items[DataPointsByExternalIdResponse]]
  implicit val errorOrStringDataPointsByIdResponseDecoder
      : Decoder[Either[CdpApiError, Items[StringDataPointsByIdResponse]]] =
    EitherDecoder.eitherDecoder[CdpApiError, Items[StringDataPointsByIdResponse]]
  implicit val errorOrStringDataPointsByExternalIdResponseDecoder
      : Decoder[Either[CdpApiError, Items[StringDataPointsByExternalIdResponse]]] =
    EitherDecoder.eitherDecoder[CdpApiError, Items[StringDataPointsByExternalIdResponse]]
  implicit val errorOrUnitDecoder: Decoder[Either[CdpApiError, Unit]] =
    EitherDecoder.eitherDecoder[CdpApiError, Unit]
  implicit val errorOrAggregateDataPointsByAggregateResponseDecoder
      : Decoder[Either[CdpApiError, Items[QueryAggregatesByIdResponse]]] =
    EitherDecoder.eitherDecoder[CdpApiError, Items[QueryAggregatesByIdResponse]]

  def insertById(id: Long, dataPoints: Seq[DataPoint]): F[Unit] =
    requestSession
      .sendCdf(
        { request =>
          request
            .post(baseUri)
            .body(
              DataPointInsertionRequest(
                Seq(
                  DataPointInsertionItem(
                    DataPointInsertionItem.IdOrExternalId.Id(id),
                    DataPointInsertionItem.DatapointType
                      .NumericDatapoints(NumericDatapoints(dataPoints.map { dp =>
                        NumericDatapoint(dp.timestamp.toEpochMilli, dp.value)
                      }))
                  )
                )
              ).toByteArray
            )
            .response(asJson[Either[CdpApiError, Unit]])
            .mapResponse {
              case Left(value) => throw value.error
              case Right(Left(cdpApiError)) => throw cdpApiError.asException(baseUri)
              case Right(Right(_)) => ()
            }
        },
        contentType = "application/protobuf"
      )

  def insertByExternalId(externalId: String, dataPoints: Seq[DataPoint]): F[Unit] =
    requestSession
      .sendCdf { request =>
        request
          .post(baseUri)
          .body(Items(Seq(DataPointsByExternalId(externalId, dataPoints))))
          .response(asJson[Either[CdpApiError, Unit]])
          .mapResponse {
            case Left(value) => throw value.error
            case Right(Left(cdpApiError)) => throw cdpApiError.asException(baseUri)
            case Right(Right(_)) => ()
          }
      }

  def insertStringsById(id: Long, dataPoints: Seq[StringDataPoint]): F[Unit] =
    requestSession
      .sendCdf(
        { request =>
          request
            .post(baseUri)
            .body(
              DataPointInsertionRequest(
                Seq(
                  DataPointInsertionItem(
                    DataPointInsertionItem.IdOrExternalId.Id(id),
                    DataPointInsertionItem.DatapointType
                      .StringDatapoints(StringDatapoints(dataPoints.map { dp =>
                        StringDatapoint(dp.timestamp.toEpochMilli, dp.value)
                      }))
                  )
                )
              ).toByteArray
            )
            .response(asJson[Either[CdpApiError, Unit]])
            .mapResponse {
              case Left(value) => throw value.error
              case Right(Left(cdpApiError)) => throw cdpApiError.asException(baseUri)
              case Right(Right(_)) => ()
            }
        },
        contentType = "application/protobuf"
      )

  def parseStringDataPoints(response: DataPointListResponse): Seq[StringDataPoint] =
    response.items
      .map(
        x =>
          x.getStringDatapoints.datapoints
            .map(s => StringDataPoint(Instant.ofEpochMilli(s.timestamp), s.value))
      )
      .headOption
      .getOrElse(Seq.empty)

  def parseNumericDataPoints(response: DataPointListResponse): Seq[DataPoint] =
    response.items
      .map(
        x => {
          x.getNumericDatapoints.datapoints
            .map(n => DataPoint(Instant.ofEpochMilli(n.timestamp), n.value))
        }
      )
      .headOption
      .getOrElse(Seq.empty)

  def screenOutNan(d: Double): Option[Double] =
    if (d.isNaN) None else Some(d)

  def parseAggregateDataPoints(response: DataPointListResponse): Seq[AggregateDataPoint] =
    response.items
      .map(
        x => {
          x.getAggregateDatapoints.datapoints
            .map(
              a =>
                AggregateDataPoint(
                  Instant.ofEpochMilli(a.timestamp),
                  screenOutNan(a.average),
                  screenOutNan(a.max),
                  screenOutNan(a.min),
                  screenOutNan(a.count),
                  screenOutNan(a.sum),
                  screenOutNan(a.interpolation),
                  screenOutNan(a.stepInterpolation),
                  screenOutNan(a.totalVariation),
                  screenOutNan(a.continuousVariance),
                  screenOutNan(a.discreteVariance)
                )
            )
        }
      )
      .headOption
      .getOrElse(Seq.empty)

  def insertStringsByExternalId(externalId: String, dataPoints: Seq[StringDataPoint]): F[Unit] =
    requestSession
      .sendCdf { request =>
        request
          .post(baseUri)
          .body(Items(Seq(StringDataPointsByExternalId(externalId, dataPoints))))
          .response(asJson[Either[CdpApiError, Unit]])
          .mapResponse {
            case Left(value) => throw value.error
            case Right(Left(cdpApiError)) => throw cdpApiError.asException(baseUri)
            case Right(Right(_)) => ()
          }
      }

  def deleteRangeById(id: Long, inclusiveStart: Instant, exclusiveEnd: Instant): F[Unit] =
    requestSession
      .sendCdf { request =>
        request
          .post(uri"$baseUri/delete")
          .body(
            Items(Seq(DeleteRangeById(id, inclusiveStart.toEpochMilli, exclusiveEnd.toEpochMilli)))
          )
          .response(asJson[Either[CdpApiError, Unit]])
          .mapResponse {
            case Left(value) => throw value.error
            case Right(Left(cdpApiError)) => throw cdpApiError.asException(uri"$baseUri/delete")
            case Right(Right(_)) => ()
          }
      }

  def deleteRangeByExternalId(
      externalId: String,
      inclusiveStart: Instant,
      exclusiveEnd: Instant
  ): F[Unit] =
    requestSession
      .sendCdf { request =>
        request
          .post(uri"$baseUri/delete")
          .body(
            Items(
              Seq(
                DeleteRangeByExternalId(
                  externalId,
                  inclusiveStart.toEpochMilli,
                  exclusiveEnd.toEpochMilli
                )
              )
            )
          )
          .response(asJson[Either[CdpApiError, Unit]])
          .mapResponse {
            case Left(value) => throw value.error
            case Right(Left(cdpApiError)) => throw cdpApiError.asException(uri"$baseUri/delete")
            case Right(Right(_)) => ()
          }
      }

  def queryById(
      id: Long,
      inclusiveStart: Instant,
      exclusiveEnd: Instant,
      limit: Option[Int] = None
  ): F[Seq[DataPoint]] = {
    val query = QueryRangeById(
      id,
      inclusiveStart.toEpochMilli.toString,
      exclusiveEnd.toEpochMilli.toString,
      Some(limit.getOrElse(Constants.dataPointsBatchSize))
    )
    queryProtobuf(Items(Seq(query)))(parseNumericDataPoints)
  }

  def queryByExternalId(
      externalId: String,
      inclusiveStart: Instant,
      exclusiveEnd: Instant,
      limit: Option[Int] = None
  ): F[Seq[DataPoint]] = {
    val query =
      QueryRangeByExternalId(
        externalId,
        inclusiveStart.toEpochMilli.toString,
        exclusiveEnd.toEpochMilli.toString,
        Some(limit.getOrElse(Constants.dataPointsBatchSize))
      )
    queryProtobuf(Items(Seq(query)))(parseNumericDataPoints)
  }

  def queryAggregatesById(
      id: Long,
      inclusiveStart: Instant,
      exclusiveEnd: Instant,
      granularity: String,
      aggregates: Seq[String],
      limit: Option[Int] = None
  ): F[Map[String, Seq[DataPoint]]] =
    queryProtobuf(
      Items(
        Seq(
          QueryRangeById(
            id,
            inclusiveStart.toEpochMilli.toString,
            exclusiveEnd.toEpochMilli.toString,
            Some(limit.getOrElse(Constants.aggregatesBatchSize)),
            Some(granularity),
            Some(aggregates)
          )
        )
      )
    ) { dataPointListResponse =>
      toAggregateMap(parseAggregateDataPoints(dataPointListResponse))
    }

  def queryAggregatesByExternalId(
      externalId: String,
      inclusiveStart: Instant,
      exclusiveEnd: Instant,
      granularity: String,
      aggregates: Seq[String],
      limit: Option[Int] = None
  ): F[Map[String, Seq[DataPoint]]] =
    queryProtobuf(
      Items(
        Seq(
          QueryRangeByExternalId(
            externalId,
            inclusiveStart.toEpochMilli.toString,
            exclusiveEnd.toEpochMilli.toString,
            Some(limit.getOrElse(Constants.aggregatesBatchSize)),
            Some(granularity),
            Some(aggregates)
          )
        )
      )
    ) { dataPointListResponse =>
      toAggregateMap(parseAggregateDataPoints(dataPointListResponse))
    }

  private def queryProtobuf[Q: Encoder, R](query: Q)(mapDataPointList: DataPointListResponse => R) =
    requestSession
      .sendCdf(
        { request =>
          request
            .post(uri"$baseUri/list")
            .body(query)
            .response(asProtobufOrCdpApiError)
            .mapResponse {
              case Left(value) => throw value.error
              case Right(Left(cdpApiError)) => throw cdpApiError.asException(uri"$baseUri/list")
              case Right(Right(dataPointListResponse)) => mapDataPointList(dataPointListResponse)
            }
        },
        accept = "application/protobuf"
      )

  def queryStringsById(
      id: Long,
      inclusiveStart: Instant,
      exclusiveEnd: Instant,
      limit: Option[Int] = None
  ): F[Seq[StringDataPoint]] = {
    val query = QueryRangeById(
      id,
      inclusiveStart.toEpochMilli.toString,
      exclusiveEnd.toEpochMilli.toString,
      Some(limit.getOrElse(Constants.dataPointsBatchSize))
    )
    queryProtobuf(Items(Seq(query)))(parseStringDataPoints)
  }

  def queryStringsByExternalId(
      externalId: String,
      inclusiveStart: Instant,
      exclusiveEnd: Instant,
      limit: Option[Int] = None
  ): F[Seq[StringDataPoint]] = {
    val query =
      QueryRangeByExternalId(
        externalId,
        inclusiveStart.toEpochMilli.toString,
        exclusiveEnd.toEpochMilli.toString,
        Some(limit.getOrElse(Constants.dataPointsBatchSize))
      )
    queryProtobuf(Items(Seq(query)))(parseStringDataPoints)
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

  def getLatestDataPointsByIds(ids: Seq[Long]): F[Map[Long, Option[DataPoint]]] =
    requestSession
      .sendCdf { request =>
        request
          .post(uri"$baseUri/latest")
          .body(Items(ids.map(CogniteId)))
          .response(asJson[Either[CdpApiError, Items[DataPointsByIdResponse]]])
          .mapResponse {
            case Left(value) => throw value.error
            case Right(Left(cdpApiError)) => throw cdpApiError.asException(uri"$baseUri/latest")
            case Right(Right(value)) =>
              value.items.map { item =>
                item.id -> item.datapoints.headOption
              }.toMap
          }
      }

  def getLatestDataPointsByExternalIds(ids: Seq[String]): F[Map[String, Option[DataPoint]]] =
    requestSession
      .sendCdf { request =>
        request
          .post(uri"$baseUri/latest")
          .body(Items(ids.map(CogniteExternalId)))
          .response(asJson[Either[CdpApiError, Items[DataPointsByExternalIdResponse]]])
          .mapResponse {
            case Left(value) => throw value.error
            case Right(Left(cdpApiError)) => throw cdpApiError.asException(uri"$baseUri/latest")
            case Right(Right(value)) =>
              value.items.map { item =>
                item.externalId -> item.datapoints.headOption
              }.toMap
          }
      }

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

  def getLatestStringDataPointByIds(ids: Seq[Long]): F[Map[Long, Option[StringDataPoint]]] =
    requestSession
      .sendCdf { request =>
        request
          .post(uri"$baseUri/latest")
          .body(Items(ids.map(CogniteId)))
          .response(asJson[Either[CdpApiError, Items[StringDataPointsByIdResponse]]])
          .mapResponse {
            case Left(value) => throw value.error
            case Right(Left(cdpApiError)) => throw cdpApiError.asException(uri"$baseUri/latest")
            case Right(Right(value)) =>
              value.items.map { item =>
                item.id -> item.datapoints.headOption
              }.toMap
          }
      }

  def getLatestStringDataPointByExternalIds(
      ids: Seq[String]
  ): F[Map[String, Option[StringDataPoint]]] =
    requestSession
      .sendCdf { request =>
        request
          .post(uri"$baseUri/latest")
          .body(Items(ids.map(CogniteExternalId)))
          .response(asJson[Either[CdpApiError, Items[StringDataPointsByExternalIdResponse]]])
          .mapResponse {
            case Left(value) => throw value.error
            case Right(Left(cdpApiError)) => throw cdpApiError.asException(uri"$baseUri/latest")
            case Right(Right(value)) =>
              value.items.map { item =>
                item.externalId -> item.datapoints.headOption
              }.toMap
          }
      }
}

object DataPointsResource {
  implicit val instantEncoder: Encoder[Instant] = Encoder.encodeLong.contramap(_.toEpochMilli)
  implicit val instantDecoder: Decoder[Instant] = Decoder.decodeLong.map(Instant.ofEpochMilli)

  implicit val cogniteIdEncoder: Encoder[CogniteId] = deriveEncoder
  implicit val cogniteIdItemsEncoder: Encoder[Items[CogniteId]] = deriveEncoder
  implicit val cogniteExternalIdEncoder: Encoder[CogniteExternalId] = deriveEncoder
  implicit val cogniteExternalIdItemsEncoder: Encoder[Items[CogniteExternalId]] = deriveEncoder
  implicit val dataPointDecoder: Decoder[DataPoint] = deriveDecoder
  implicit val dataPointEncoder: Encoder[DataPoint] = deriveEncoder
  implicit val dataPointsByIdResponseDecoder: Decoder[DataPointsByIdResponse] = deriveDecoder
  implicit val dataPointsByIdResponseItemsDecoder: Decoder[Items[DataPointsByIdResponse]] =
    deriveDecoder
  implicit val dataPointsByExternalIdResponseDecoder: Decoder[DataPointsByExternalIdResponse] =
    deriveDecoder
  implicit val dataPointsByExternalIdResponseItemsDecoder
      : Decoder[Items[DataPointsByExternalIdResponse]] =
    deriveDecoder

  // WartRemover gets confused by circe-derivation
  @SuppressWarnings(Array("org.wartremover.warts.JavaSerializable"))
  implicit val stringDataPointDecoder: Decoder[StringDataPoint] = deriveDecoder
  implicit val stringDataPointEncoder: Encoder[StringDataPoint] = deriveEncoder
  implicit val stringDataPointsByIdResponseDecoder: Decoder[StringDataPointsByIdResponse] =
    deriveDecoder
  implicit val stringDataPointsByIdResponseItemsDecoder
      : Decoder[Items[StringDataPointsByIdResponse]] = deriveDecoder
  implicit val stringDataPointsByExternalIdResponseDecoder
      : Decoder[StringDataPointsByExternalIdResponse] =
    deriveDecoder
  implicit val stringDataPointsByExternalIdResponseItemsDecoder
      : Decoder[Items[StringDataPointsByExternalIdResponse]] = deriveDecoder
  implicit val dataPointsByIdEncoder: Encoder[DataPointsById] = deriveEncoder
  implicit val dataPointsByIdItemsEncoder: Encoder[Items[DataPointsById]] = deriveEncoder
  implicit val dataPointsByExternalIdEncoder: Encoder[DataPointsByExternalId] = deriveEncoder
  implicit val dataPointsByExternalIdItemsEncoder: Encoder[Items[DataPointsByExternalId]] =
    deriveEncoder
  implicit val stringDataPointsByIdEncoder: Encoder[StringDataPointsById] = deriveEncoder
  implicit val stringDataPointsByIdItemsEncoder: Encoder[Items[StringDataPointsById]] =
    deriveEncoder
  implicit val stringDataPointsByExternalIdEncoder: Encoder[StringDataPointsByExternalId] =
    deriveEncoder
  implicit val stringDataPointsByExternalIdItemsEncoder
      : Encoder[Items[StringDataPointsByExternalId]] = deriveEncoder
  implicit val deleteRangeByIdEncoder: Encoder[DeleteRangeById] = deriveEncoder
  implicit val deleteRangeByIdItemsEncoder: Encoder[Items[DeleteRangeById]] = deriveEncoder
  implicit val deleteRangeByExternalIdEncoder: Encoder[DeleteRangeByExternalId] = deriveEncoder
  implicit val deleteRangeByExternalIdItemsEncoder: Encoder[Items[DeleteRangeByExternalId]] =
    deriveEncoder
  implicit val queryRangeByIdEncoder: Encoder[QueryRangeById] = deriveEncoder
  implicit val queryRangeByIdItemsEncoder: Encoder[Items[QueryRangeById]] = deriveEncoder
  implicit val queryRangeByExternalIdEncoder: Encoder[QueryRangeByExternalId] = deriveEncoder
  implicit val queryRangeByExternalIdItemsEncoder: Encoder[Items[QueryRangeByExternalId]] =
    deriveEncoder
  @SuppressWarnings(Array("org.wartremover.warts.JavaSerializable"))
  implicit val aggregateDataPointDecoder: Decoder[AggregateDataPoint] = deriveDecoder
  implicit val aggregateDataPointEncoder: Encoder[AggregateDataPoint] = deriveEncoder
  implicit val queryAggregatesByIdResponseEncoder: Encoder[QueryAggregatesByIdResponse] =
    deriveEncoder
  implicit val queryAggregatesByIdResponseDecoder: Decoder[QueryAggregatesByIdResponse] =
    deriveDecoder
  implicit val queryAggregatesByIdResponseItemsDecoder
      : Decoder[Items[QueryAggregatesByIdResponse]] =
    deriveDecoder

  val asProtobufOrCdpApiError: ResponseAs[
    Either[DeserializationError[io.circe.Error], Either[CdpApiError, DataPointListResponse]],
    Nothing
  ] = {
    asByteArray.map(response => {
      // TODO: Can use the HTTP headers in .mapWithMetaData to choose to parse as json or protbuf
      try {
        Right(Right(DataPointListResponse.parseFrom(response)))
      } catch {
        case _: Throwable =>
          val s = new String(response, StandardCharsets.UTF_8)
          decode[CdpApiError](s) match {
            case Left(error) =>
              Left(DeserializationError(s, error, Show[io.circe.Error].show(error)))
            case Right(cdpApiError) => Right(Left(cdpApiError))
          }
      }
    })
  }

  private def toAggregateMap(
      aggregateDataPoints: Seq[AggregateDataPoint]
  ): Map[String, Seq[DataPoint]] =
    Map(
      "average" ->
        aggregateDataPoints.flatMap(p => p.average.toList.map(v => DataPoint(p.timestamp, v))),
      "max" ->
        aggregateDataPoints.flatMap(p => p.max.toList.map(v => DataPoint(p.timestamp, v))),
      "min" ->
        aggregateDataPoints.flatMap(p => p.min.toList.map(v => DataPoint(p.timestamp, v))),
      "count" ->
        aggregateDataPoints.flatMap(p => p.count.toList.map(v => DataPoint(p.timestamp, v))),
      "sum" ->
        aggregateDataPoints.flatMap(p => p.sum.toList.map(v => DataPoint(p.timestamp, v))),
      "interpolation" ->
        aggregateDataPoints.flatMap(
          p => p.interpolation.toList.map(v => DataPoint(p.timestamp, v))
        ),
      "stepInterpolation" ->
        aggregateDataPoints.flatMap(
          p => p.stepInterpolation.toList.map(v => DataPoint(p.timestamp, v))
        ),
      "continuousVariance" ->
        aggregateDataPoints.flatMap(
          p => p.continuousVariance.toList.map(v => DataPoint(p.timestamp, v))
        ),
      "discreteVariance" ->
        aggregateDataPoints.flatMap(
          p => p.discreteVariance.toList.map(v => DataPoint(p.timestamp, v))
        ),
      "totalVariation" ->
        aggregateDataPoints.flatMap(
          p => p.totalVariation.toList.map(v => DataPoint(p.timestamp, v))
        )
    ).filter(kv => kv._2.nonEmpty)
}
