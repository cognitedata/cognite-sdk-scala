package com.cognite.sdk.scala.v1.resources

import com.cognite.sdk.scala.common._
import com.cognite.sdk.scala.v1._
import com.softwaremill.sttp._
import com.softwaremill.sttp.circe._
import io.circe.generic.semiauto._
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

  def insertById(id: Long, dataPoints: Seq[DataPoint]): F[Response[Unit]] =
    requestSession
      .send { request =>
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

  def insertStringsById(id: Long, dataPoints: Seq[StringDataPoint]): F[Response[Unit]] =
    requestSession
      .send { request =>
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

  def deleteRangeById(id: Long, inclusiveStart: Long, exclusiveEnd: Long): F[Response[Unit]] =
    requestSession
      .send { request =>
        request
          .post(uri"$baseUri/delete")
          .body(Items(Seq(DeleteRangeById(id, inclusiveStart, exclusiveEnd))))
          .response(asJson[Either[CdpApiError, Unit]])
          .mapResponse {
            case Left(value) => throw value.error
            case Right(Left(cdpApiError)) => throw cdpApiError.asException(baseUri)
            case Right(Right(_)) => ()
          }
      }

  def queryById(id: Long, inclusiveStart: Long, exclusiveEnd: Long): F[Response[Seq[DataPoint]]] =
    requestSession
      .send { request =>
        request
          .post(uri"$baseUri/list")
          .body(Items(Seq(QueryRangeById(id, inclusiveStart.toString, exclusiveEnd.toString))))
          .response(asJson[Either[CdpApiError, Items[DataPointsByIdResponse]]])
          .mapResponse {
            case Left(value) => throw value.error
            case Right(Left(cdpApiError)) => throw cdpApiError.asException(baseUri)
            case Right(Right(value)) =>
              value.items.headOption match {
                case Some(items) => items.datapoints
                case None => Seq.empty
              }
          }
      }

  def queryStringsById(
      id: Long,
      inclusiveStart: Long,
      exclusiveEnd: Long
  ): F[Response[Seq[StringDataPoint]]] =
    requestSession
      .send { request =>
        request
          .post(uri"$baseUri/list")
          .body(Items(Seq(QueryRangeById(id, inclusiveStart.toString, exclusiveEnd.toString))))
          .response(asJson[Either[CdpApiError, Items[StringDataPointsByIdResponse]]])
          .mapResponse {
            case Left(value) => throw value.error
            case Right(Left(cdpApiError)) => throw cdpApiError.asException(baseUri)
            case Right(Right(value)) =>
              value.items.headOption match {
                case Some(items) => items.datapoints
                case None => Seq.empty
              }
          }
      }

  //def deleteRangeByExternalId(start: Long, end: Long, externalId: String): F[Response[Unit]]
  def getLatestDataPointById(id: Long): F[Response[Option[DataPoint]]] =
    requestSession
      .send { request =>
        request
          .post(uri"$baseUri/latest")
          .body(Items(Seq(CogniteId(id))))
          .response(asJson[Either[CdpApiError, Items[DataPointsByIdResponse]]])
          .mapResponse {
            case Left(value) => throw value.error
            case Right(Left(cdpApiError)) => throw cdpApiError.asException(baseUri)
            case Right(Right(value)) =>
              value.items.headOption.flatMap(_.datapoints.headOption)
          }
      }

  def getLatestStringDataPointById(id: Long): F[Response[Option[StringDataPoint]]] =
    requestSession
      .send { request =>
        request
          .post(uri"$baseUri/latest")
          .body(Items(Seq(CogniteId(id))))
          .response(asJson[Either[CdpApiError, Items[StringDataPointsByIdResponse]]])
          .mapResponse {
            case Left(value) => throw value.error
            case Right(Left(cdpApiError)) => throw cdpApiError.asException(baseUri)
            case Right(Right(value)) =>
              value.items.headOption.flatMap(_.datapoints.headOption)
          }
      }
}

object DataPointsResourceV1 {
  implicit val cogniteIdEncoder: Encoder[CogniteId] = deriveEncoder
  implicit val cogniteIdItemsEncoder: Encoder[Items[CogniteId]] = deriveEncoder
  implicit val dataPointDecoder: Decoder[DataPoint] = deriveDecoder
  implicit val dataPointEncoder: Encoder[DataPoint] = deriveEncoder
  implicit val dataPointsByIdResponseDecoder: Decoder[DataPointsByIdResponse] = deriveDecoder
  implicit val dataPointsByIdResponseItemsDecoder: Decoder[Items[DataPointsByIdResponse]] = deriveDecoder
  implicit val stringDataPointDecoder: Decoder[StringDataPoint] = deriveDecoder
  implicit val stringDataPointEncoder: Encoder[StringDataPoint] = deriveEncoder
  implicit val stringDataPointsByIdResponseDecoder: Decoder[StringDataPointsByIdResponse] = deriveDecoder
  implicit val stringDataPointsByIdResponseItemsDecoder: Decoder[Items[StringDataPointsByIdResponse]] = deriveDecoder
  implicit val dataPointsByIdEncoder: Encoder[DataPointsById] = deriveEncoder
  implicit val dataPointsByIdItemsEncoder: Encoder[Items[DataPointsById]] = deriveEncoder
  implicit val stringDataPointsByIdEncoder: Encoder[StringDataPointsById] = deriveEncoder
  implicit val stringDataPointsByIdItemsEncoder: Encoder[Items[StringDataPointsById]] = deriveEncoder
  implicit val deleteRangeByIdEncoder: Encoder[DeleteRangeById] = deriveEncoder
  implicit val deleteRangeByIdItemsEncoder: Encoder[Items[DeleteRangeById]] = deriveEncoder
  implicit val queryRangeByIdEncoder: Encoder[QueryRangeById] = deriveEncoder
  implicit val queryRangeByIdItemsEncoder: Encoder[Items[QueryRangeById]] = deriveEncoder
}
