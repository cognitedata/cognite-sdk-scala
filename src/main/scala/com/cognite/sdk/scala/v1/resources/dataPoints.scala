package com.cognite.sdk.scala.v1.resources

import com.cognite.sdk.scala.common._
import com.cognite.sdk.scala.v1.{
  DataPointsById,
  DataPointsByIdResponse,
  DeleteRangeById,
  QueryRangeById,
  StringDataPointsById,
  StringDataPointsByIdResponse
}
import com.softwaremill.sttp._
import com.softwaremill.sttp.circe._
import io.circe.Decoder
import io.circe.generic.auto._

class DataPointsResourceV1[F[_]](val requestSession: RequestSession)(
    implicit sttpBackend: SttpBackend[F, _]
) extends WithRequestSession
    with BaseUri
    with DataPointsResource[F, Long] {
  override val baseUri = uri"${requestSession.baseUri}/timeseries/data"

  implicit val errorOrUnitDecoder: Decoder[Either[CdpApiError, Unit]] =
    EitherDecoder.eitherDecoder[CdpApiError, Unit]
  implicit val errorOrDataPointsByIdResponseDecoder
      : Decoder[Either[CdpApiError, Items[DataPointsByIdResponse]]] =
    EitherDecoder.eitherDecoder[CdpApiError, Items[DataPointsByIdResponse]]
  implicit val errorOrStringDataPointsByIdResponseDecoder
      : Decoder[Either[CdpApiError, Items[StringDataPointsByIdResponse]]] =
    EitherDecoder.eitherDecoder[CdpApiError, Items[StringDataPointsByIdResponse]]

  def insertById(id: Long, dataPoints: Seq[DataPoint]): F[Response[Unit]] =
    requestSession
      .request
      .post(baseUri)
      .body(Items(Seq(DataPointsById(id, dataPoints))))
      .response(asJson[Either[CdpApiError, Unit]])
      .mapResponse {
        case Left(value) => throw value.error
        case Right(Left(cdpApiError)) => throw cdpApiError.asException(baseUri)
        case Right(Right(_)) => ()
      }
      .send()

  def insertStringsById(id: Long, dataPoints: Seq[StringDataPoint]): F[Response[Unit]] =
    requestSession
      .request
      .post(baseUri)
      .body(Items(Seq(StringDataPointsById(id, dataPoints))))
      .response(asJson[Either[CdpApiError, Unit]])
      .mapResponse {
        case Left(value) => throw value.error
        case Right(Left(cdpApiError)) => throw cdpApiError.asException(baseUri)
        case Right(Right(_)) => ()
      }
      .send()

  def deleteRangeById(id: Long, inclusiveStart: Long, exclusiveEnd: Long): F[Response[Unit]] =
    requestSession
      .request
      .post(uri"$baseUri/delete")
      .body(Items(Seq(DeleteRangeById(id, inclusiveStart, exclusiveEnd))))
      .response(asJson[Either[CdpApiError, Unit]])
      .mapResponse {
        case Left(value) => throw value.error
        case Right(Left(cdpApiError)) => throw cdpApiError.asException(baseUri)
        case Right(Right(_)) => ()
      }
      .send()

  def queryById(id: Long, inclusiveStart: Long, exclusiveEnd: Long): F[Response[Seq[DataPoint]]] =
    requestSession
      .request
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
      .send()

  def queryStringsById(
      id: Long,
      inclusiveStart: Long,
      exclusiveEnd: Long
  ): F[Response[Seq[StringDataPoint]]] =
    requestSession
      .request
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
      .send()

  //def deleteRangeByExternalId(start: Long, end: Long, externalId: String): F[Response[Unit]]
  def getLatestDataPointById(id: Long): F[Response[Option[DataPoint]]] =
    requestSession
      .request
      .post(uri"$baseUri/latest")
      .body(Items(Seq(CogniteId(id))))
      .response(asJson[Either[CdpApiError, Items[DataPointsByIdResponse]]])
      .mapResponse {
        case Left(value) => throw value.error
        case Right(Left(cdpApiError)) => throw cdpApiError.asException(baseUri)
        case Right(Right(value)) =>
          value.items.headOption.flatMap(_.datapoints.headOption)
      }
      .send()

  def getLatestStringDataPointById(id: Long): F[Response[Option[StringDataPoint]]] =
    requestSession
      .request
      .post(uri"$baseUri/latest")
      .body(Items(Seq(CogniteId(id))))
      .response(asJson[Either[CdpApiError, Items[StringDataPointsByIdResponse]]])
      .mapResponse {
        case Left(value) => throw value.error
        case Right(Left(cdpApiError)) => throw cdpApiError.asException(baseUri)
        case Right(Right(value)) =>
          value.items.headOption.flatMap(_.datapoints.headOption)
      }
      .send()
}

object DataPointsResourceV1 {
  // do something like add support for insert multiple?
}
