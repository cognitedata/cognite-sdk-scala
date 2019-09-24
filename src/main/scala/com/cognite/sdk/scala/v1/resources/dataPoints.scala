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
import io.circe.parser.decode

class DataPointsResourceV1[F[_]](val requestSession: RequestSession[F])
    extends WithRequestSession[F]
    with BaseUri
    with DataPointsResource[F, Long] {

  import DataPointsResourceV1._

  override val baseUri = uri"${requestSession.baseUri}/timeseries/data"

  implicit val errorOrDataPointsByIdResponseDecoder
      : Decoder[Either[CdpApiError, Items[DataPointsByIdResponse]]] =
    EitherDecoder.eitherDecoder[CdpApiError, Items[DataPointsByIdResponse]]
  implicit val errorOrStringDataPointsByIdResponseDecoder
      : Decoder[Either[CdpApiError, Items[StringDataPointsByIdResponse]]] =
    EitherDecoder.eitherDecoder[CdpApiError, Items[StringDataPointsByIdResponse]]
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
                  screenOutNan(a.min),
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

  def deleteRangeById(id: Long, inclusiveStart: Long, exclusiveEnd: Long): F[Unit] =
    requestSession
      .sendCdf { request =>
        request
          .post(uri"$baseUri/delete")
          .body(Items(Seq(DeleteRangeById(id, inclusiveStart, exclusiveEnd))))
          .response(asJson[Either[CdpApiError, Unit]])
          .mapResponse {
            case Left(value) => throw value.error
            case Right(Left(cdpApiError)) => throw cdpApiError.asException(uri"$baseUri/delete")
            case Right(Right(_)) => ()
          }
      }

  def queryById(id: Long, inclusiveStart: Long, exclusiveEnd: Long): F[Seq[DataPoint]] =
    requestSession
      .sendCdf(
        { request =>
          request
            .post(uri"$baseUri/list")
            .body(Items(Seq(QueryRangeById(id, inclusiveStart.toString, exclusiveEnd.toString))))
            .response(asProtobufOrCdpApiError)
            .mapResponse {
              case Left(value) => throw value.error
              case Right(Left(cdpApiError)) => throw cdpApiError.asException(uri"$baseUri/delete")
              case Right(Right(dataPointListResponse)) =>
                parseNumericDataPoints(dataPointListResponse)
            }
        },
        accept = "application/protobuf"
      )

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

  def queryAggregatesById(
      id: Long,
      inclusiveStart: Long,
      exclusiveEnd: Long,
      granularity: String,
      aggregates: Seq[String]
  ): F[Map[String, Seq[DataPoint]]] =
    requestSession
      .sendCdf(
        { request =>
          request
            .post(uri"$baseUri/list")
            .body(
              Items(
                Seq(
                  QueryAggregatesById(
                    id,
                    inclusiveStart.toString,
                    exclusiveEnd.toString,
                    granularity,
                    aggregates
                  )
                )
              )
            )
            .response(asProtobufOrCdpApiError)
            .mapResponse {
              case Left(value) => throw value.error
              case Right(Left(cdpApiError)) => throw cdpApiError.asException(uri"$baseUri/list")
              case Right(Right(dataPointListResponse)) =>
                toAggregateMap(parseAggregateDataPoints(dataPointListResponse))
            }
        },
        accept = "application/protobuf"
      )

  def queryAggregatesByExternalId(
      externalId: String,
      inclusiveStart: Long,
      exclusiveEnd: Long,
      granularity: String,
      aggregates: Seq[String]
  ): F[Map[String, Seq[DataPoint]]] =
    requestSession
      .sendCdf(
        { request =>
          request
            .post(uri"$baseUri/list")
            .body(
              Items(
                Seq(
                  QueryAggregatesByExternalId(
                    externalId,
                    inclusiveStart.toString,
                    exclusiveEnd.toString,
                    granularity,
                    aggregates
                  )
                )
              )
            )
            .response(asProtobufOrCdpApiError)
            .mapResponse {
              case Left(value) => throw value.error
              case Right(Left(cdpApiError)) => throw cdpApiError.asException(uri"$baseUri/list")
              case Right(Right(dataPointListResponse)) =>
                toAggregateMap(parseAggregateDataPoints(dataPointListResponse))
            }
        },
        accept = "application/protobuf"
      )

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

  def queryStringsById(
      id: Long,
      inclusiveStart: Long,
      exclusiveEnd: Long
  ): F[Seq[StringDataPoint]] =
    requestSession
      .sendCdf(
        { request =>
          request
            .post(uri"$baseUri/list")
            .body(Items(Seq(QueryRangeById(id, inclusiveStart.toString, exclusiveEnd.toString))))
            .response(asProtobufOrCdpApiError)
            .mapResponse {
              case Left(value) => throw value.error
              case Right(Left(cdpApiError)) => throw cdpApiError.asException(uri"$baseUri/delete")
              case Right(Right(dataPointListResponse)) =>
                parseStringDataPoints(dataPointListResponse)
            }
        },
        accept = "application/protobuf"
      )

  //def deleteRangeByExternalId(start: Long, end: Long, externalId: String): F[Response[Unit]]
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
}

object DataPointsResourceV1 {
  implicit val instantEncoder: Encoder[Instant] = Encoder.encodeLong.contramap(_.toEpochMilli)
  implicit val instantDecoder: Decoder[Instant] = Decoder.decodeLong.map(Instant.ofEpochMilli)

  implicit val cogniteIdEncoder: Encoder[CogniteId] = deriveEncoder
  implicit val cogniteIdItemsEncoder: Encoder[Items[CogniteId]] = deriveEncoder
  implicit val dataPointDecoder: Decoder[DataPoint] = deriveDecoder
  implicit val dataPointEncoder: Encoder[DataPoint] = deriveEncoder
  implicit val dataPointsByIdResponseDecoder: Decoder[DataPointsByIdResponse] = deriveDecoder
  implicit val dataPointsByIdResponseItemsDecoder: Decoder[Items[DataPointsByIdResponse]] =
    deriveDecoder

  // WartRemover gets confused by circe-derivation
  @SuppressWarnings(Array("org.wartremover.warts.JavaSerializable"))
  implicit val stringDataPointDecoder: Decoder[StringDataPoint] = deriveDecoder
  implicit val stringDataPointEncoder: Encoder[StringDataPoint] = deriveEncoder
  implicit val stringDataPointsByIdResponseDecoder: Decoder[StringDataPointsByIdResponse] =
    deriveDecoder
  implicit val stringDataPointsByIdResponseItemsDecoder
      : Decoder[Items[StringDataPointsByIdResponse]] = deriveDecoder
  implicit val dataPointsByIdEncoder: Encoder[DataPointsById] = deriveEncoder
  implicit val dataPointsByIdItemsEncoder: Encoder[Items[DataPointsById]] = deriveEncoder
  implicit val stringDataPointsByIdEncoder: Encoder[StringDataPointsById] = deriveEncoder
  implicit val stringDataPointsByIdItemsEncoder: Encoder[Items[StringDataPointsById]] =
    deriveEncoder
  implicit val deleteRangeByIdEncoder: Encoder[DeleteRangeById] = deriveEncoder
  implicit val deleteRangeByIdItemsEncoder: Encoder[Items[DeleteRangeById]] = deriveEncoder
  implicit val queryRangeByIdEncoder: Encoder[QueryRangeById] = deriveEncoder
  implicit val queryRangeByIdItemsEncoder: Encoder[Items[QueryRangeById]] = deriveEncoder
  @SuppressWarnings(Array("org.wartremover.warts.JavaSerializable"))
  implicit val aggregateDataPointDecoder: Decoder[AggregateDataPoint] = deriveDecoder
  implicit val aggregateDataPointEncoder: Encoder[AggregateDataPoint] = deriveEncoder
  implicit val queryAggregatesByIdResponseEncoder: Encoder[QueryAggregatesByIdResponse] =
    deriveEncoder
  implicit val queryAggregatesByIdResponseDecoder: Decoder[QueryAggregatesByIdResponse] =
    deriveDecoder
  implicit val queryAggregatesByIdEncoder: Encoder[QueryAggregatesById] = deriveEncoder
  implicit val queryAggregatesByExternalIdEncoder: Encoder[QueryAggregatesByExternalId] =
    deriveEncoder
  implicit val queryAggregatesByIdItemsEncoder: Encoder[Items[QueryAggregatesById]] = deriveEncoder
  implicit val queryAggregatesByExternalIdItemsEncoder
      : Encoder[Items[QueryAggregatesByExternalId]] = deriveEncoder
  implicit val queryAggregatesByIdResponseItemsDecoder
      : Decoder[Items[QueryAggregatesByIdResponse]] =
    deriveDecoder

}
