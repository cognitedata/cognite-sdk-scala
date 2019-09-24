package com.cognite.sdk.scala.v1.resources

import java.time.Instant

import com.cognite.sdk.scala.common._
import com.cognite.sdk.scala.v1._
import com.softwaremill.sttp._
import com.softwaremill.sttp.circe._
import io.circe.derivation.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

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
      .sendCdf { request =>
        request
          .post(baseUri)
          .body(Items(Seq(DataPointsById(id, dataPoints))))
          .response(asJson[Either[CdpApiError, Unit]])
          .mapResponse {
            case Left(value) => throw value.error
            case Right(Left(cdpApiError)) => throw cdpApiError.asException(baseUri)
            case Right(Right(_)) => ()
          }
      }

  def insertStringsById(id: Long, dataPoints: Seq[StringDataPoint]): F[Unit] =
    requestSession
      .sendCdf { request =>
        request
          .post(baseUri)
          .body(Items(Seq(StringDataPointsById(id, dataPoints))))
          .response(asJson[Either[CdpApiError, Unit]])
          .mapResponse {
            case Left(value) => throw value.error
            case Right(Left(cdpApiError)) => throw cdpApiError.asException(baseUri)
            case Right(Right(_)) => ()
          }
      }

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
      .sendCdf { request =>
        request
          .post(uri"$baseUri/list")
          .body(Items(Seq(QueryRangeById(id, inclusiveStart.toString, exclusiveEnd.toString))))
          .response(asJson[Either[CdpApiError, Items[DataPointsByIdResponse]]])
          .mapResponse {
            case Left(value) => throw value.error
            case Right(Left(cdpApiError)) => throw cdpApiError.asException(uri"$baseUri/list")
            case Right(Right(value)) =>
              value.items.headOption match {
                case Some(items) => items.datapoints
                case None => Seq.empty
              }
          }
      }

  def queryAggregatesById(
      id: Long,
      inclusiveStart: Long,
      exclusiveEnd: Long,
      granularity: String,
      aggregates: Seq[String]
  ): F[Map[String, Seq[DataPoint]]] =
    requestSession
      .sendCdf { request =>
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
          .response(asJson[Either[CdpApiError, Items[QueryAggregatesByIdResponse]]])
          .mapResponse {
            case Left(value) => throw value.error
            case Right(Left(cdpApiError)) => throw cdpApiError.asException(uri"$baseUri/list")
            case Right(Right(value)) =>
              value.items.headOption
                .map(i => toAggregateMap(i.datapoints))
                .getOrElse(Map.empty)
          }
      }

  def queryAggregatesByExternalId(
      externalId: String,
      inclusiveStart: Long,
      exclusiveEnd: Long,
      granularity: String,
      aggregates: Seq[String]
  ): F[Map[String, Seq[DataPoint]]] =
    requestSession
      .sendCdf { request =>
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
          .response(asJson[Either[CdpApiError, Items[QueryAggregatesByIdResponse]]])
          .mapResponse {
            case Left(value) => throw value.error
            case Right(Left(cdpApiError)) => throw cdpApiError.asException(uri"$baseUri/list")
            case Right(Right(value)) =>
              value.items.headOption
                .map(i => toAggregateMap(i.datapoints))
                .getOrElse(Map.empty)
          }
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

  def queryStringsById(
      id: Long,
      inclusiveStart: Long,
      exclusiveEnd: Long
  ): F[Seq[StringDataPoint]] =
    requestSession
      .sendCdf { request =>
        request
          .post(uri"$baseUri/list")
          .body(Items(Seq(QueryRangeById(id, inclusiveStart.toString, exclusiveEnd.toString))))
          .response(asJson[Either[CdpApiError, Items[StringDataPointsByIdResponse]]])
          .mapResponse {
            case Left(value) => throw value.error
            case Right(Left(cdpApiError)) => throw cdpApiError.asException(uri"$baseUri/list")
            case Right(Right(value)) =>
              value.items.headOption match {
                case Some(items) => items.datapoints
                case None => Seq.empty
              }
          }
      }

  //def deleteRangeByExternalId(start: Long, end: Long, externalId: String): F[Response[Unit]]
  def getLatestDataPointById(id: Long): F[Option[DataPoint]] =
    requestSession.map(
      getLatestDataPointsByIds(Seq(id)),g
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
