package com.cognite.sdk.scala.v1_0

import com.cognite.sdk.scala.common.{
  Auth,
  CdpApiError,
  CogniteId,
  DataPoint,
  DataPointsResource,
  EitherDecoder,
  Items,
  Resource,
  StringDataPoint
}
import com.softwaremill.sttp._
import com.softwaremill.sttp.circe._
import io.circe.Decoder
import io.circe.generic.auto._

final case class DataPointsById(
    id: Long,
    datapoints: Seq[DataPoint]
)

final case class DataPointsByIdResponse(
    id: Long,
    externalId: Option[String],
    isString: Boolean,
    datapoints: Seq[DataPoint]
)

final case class StringDataPointsByIdResponse(
    id: Long,
    externalId: Option[String],
    isString: Boolean,
    datapoints: Seq[StringDataPoint]
)

final case class StringDataPointsById(
    id: Long,
    datapoints: Seq[StringDataPoint]
)

final case class DeleteRangeById(
    id: Long,
    inclusiveBegin: Long,
    exclusiveEnd: Long
)

final case class DeleteRangeByExternalId(
    id: String,
    inclusiveBegin: Long,
    exclusiveEnd: Long
)

final case class QueryRangeById(
    id: Long,
    start: String,
    end: String
)

class DataPointsResourceV1[F[_]](project: String)(
    implicit auth: Auth,
    sttpBackend: SttpBackend[F, _]
) extends Resource[F, CogniteId, Long](auth)
    with ResourceV1[F]
    with DataPointsResource[F, Long] {
  override val baseUri = uri"https://api.cognitedata.com/api/v1/projects/$project/timeseries/data"

  override def toInternalId(id: Long): CogniteId = CogniteId(id)
  implicit val errorOrUnitDecoder: Decoder[Either[CdpApiError[CogniteId], Unit]] =
    EitherDecoder.eitherDecoder[CdpApiError[CogniteId], Unit]
  implicit val errorOrDataPointsByIdResponseDecoder
      : Decoder[Either[CdpApiError[CogniteId], Items[DataPointsByIdResponse]]] =
    EitherDecoder.eitherDecoder[CdpApiError[CogniteId], Items[DataPointsByIdResponse]]
  implicit val errorOrStringDataPointsByIdResponseDecoder
      : Decoder[Either[CdpApiError[CogniteId], Items[StringDataPointsByIdResponse]]] =
    EitherDecoder.eitherDecoder[CdpApiError[CogniteId], Items[StringDataPointsByIdResponse]]

  def insertById(id: Long, dataPoints: Seq[DataPoint]): F[Response[Unit]] =
    request
      .post(baseUri)
      .body(Items(Seq(DataPointsById(id, dataPoints))))
//      .response(asJson[Either[CdpApiError[CogniteId], Unit]])
//      .mapResponse {
//        case Left(value) =>
//          print("kaboom") // scalastyle:ignore
//          print(value.original) // scalastyle:ignore
//          print(value.error.toString) // scalastyle:ignore
//          throw value.error
//        case Right(Left(cdpApiError)) =>
//          throw cdpApiError.asException(baseUri)
//        case Right(Right(_)) => ()
//      }
      .response(asJson[Either[CdpApiError[CogniteId], Unit]])
      .mapResponse {
        case Left(value) => throw value.error
        case Right(Left(cdpApiError)) => throw cdpApiError.asException(baseUri)
        case Right(Right(_)) => ()
      }
      .send()

  def insertStringsById(id: Long, dataPoints: Seq[StringDataPoint]): F[Response[Unit]] =
    request
      .post(baseUri)
      .body(Items(Seq(StringDataPointsById(id, dataPoints))))
      .response(asJson[Either[CdpApiError[CogniteId], Unit]])
      .mapResponse {
        case Left(value) => throw value.error
        case Right(Left(cdpApiError)) => throw cdpApiError.asException(baseUri)
        case Right(Right(_)) => ()
      }
      .send()

  def deleteRangeById(id: Long, inclusiveStart: Long, exclusiveEnd: Long): F[Response[Unit]] =
    request
      .post(uri"$baseUri/delete")
      .body(Items(Seq(DeleteRangeById(id, inclusiveStart, exclusiveEnd))))
      .response(asJson[Either[CdpApiError[CogniteId], Unit]])
      .mapResponse {
        case Left(value) => throw value.error
        case Right(Left(cdpApiError)) => throw cdpApiError.asException(baseUri)
        case Right(Right(_)) => ()
      }
      .send()

  def queryById(id: Long, inclusiveStart: Long, exclusiveEnd: Long): F[Response[Seq[DataPoint]]] =
    request
      .post(uri"$baseUri/list")
      .body(Items(Seq(QueryRangeById(id, inclusiveStart.toString, exclusiveEnd.toString))))
      .response(asJson[Either[CdpApiError[CogniteId], Items[DataPointsByIdResponse]]])
      .mapResponse {
        case Left(value) => throw value.error
        case Right(Left(cdpApiError)) => throw cdpApiError.asException(baseUri)
        case Right(Right(value)) =>
          extractor.extract(value).items.headOption match {
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
    request
      .post(uri"$baseUri/list")
      .body(Items(Seq(QueryRangeById(id, inclusiveStart.toString, exclusiveEnd.toString))))
      .response(asJson[Either[CdpApiError[CogniteId], Items[StringDataPointsByIdResponse]]])
      .mapResponse {
        case Left(value) => throw value.error
        case Right(Left(cdpApiError)) => throw cdpApiError.asException(baseUri)
        case Right(Right(value)) =>
          extractor.extract(value).items.headOption match {
            case Some(items) => items.datapoints
            case None => Seq.empty
          }
      }
      .send()

  //def deleteRangeByExternalId(start: Long, end: Long, externalId: String): F[Response[Unit]]
  def getLatestDataPointById(id: Long): F[Response[Option[DataPoint]]] =
    request
      .post(uri"$baseUri/latest")
      .body(Items(Seq(CogniteId(id))))
      .response(asJson[Either[CdpApiError[CogniteId], Items[DataPointsByIdResponse]]])
      .mapResponse {
        case Left(value) => throw value.error
        case Right(Left(cdpApiError)) => throw cdpApiError.asException(baseUri)
        case Right(Right(value)) =>
          extractor.extract(value).items.headOption.flatMap(_.datapoints.headOption)
      }
      .send()

  def getLatestStringDataPointById(id: Long): F[Response[Option[StringDataPoint]]] =
    request
      .post(uri"$baseUri/latest")
      .body(Items(Seq(CogniteId(id))))
      .response(asJson[Either[CdpApiError[CogniteId], Items[StringDataPointsByIdResponse]]])
      .mapResponse {
        case Left(value) => throw value.error
        case Right(Left(cdpApiError)) => throw cdpApiError.asException(baseUri)
        case Right(Right(value)) =>
          extractor.extract(value).items.headOption.flatMap(_.datapoints.headOption)
      }
      .send()
}

object DataPointsResourceV1 {
  // do something like add support for insert multiple?
}
