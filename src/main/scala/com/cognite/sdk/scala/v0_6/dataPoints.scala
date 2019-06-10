package com.cognite.sdk.scala.v0_6

import com.cognite.sdk.scala.common._
import com.softwaremill.sttp._
import com.softwaremill.sttp.circe._
import io.circe.Decoder
import io.circe.generic.auto._

final case class DataPointsByName(
    name: String,
    datapoints: Seq[DataPoint]
)

final case class StringDataPointsByName(
    name: String,
    datapoints: Seq[StringDataPoint]
)

class DataPointsResourceV0_6[F[_]](implicit auth: Auth, sttpBackend: SttpBackend[F, _])
    extends Resource[F, Long, Long](auth)
    with ResourceV0_6[F]
    with DataPointsResource[F, Long] {
  override val baseUri = uri"https://api.cognitedata.com/api/0.6/projects/playground/timeseries"

  implicit val errorOrUnitDecoder: Decoder[Either[CdpApiError[CogniteId], Unit]] =
    EitherDecoder.eitherDecoder[CdpApiError[CogniteId], Unit]
  implicit val errorOrDataPointsByNameResponseDecoder
      : Decoder[Either[CdpApiError[CogniteId], Data[Items[DataPointsByName]]]] =
    EitherDecoder.eitherDecoder[CdpApiError[CogniteId], Data[Items[DataPointsByName]]]
  implicit val errorOrStringDataPointsByNameResponseDecoder
      : Decoder[Either[CdpApiError[CogniteId], Data[Items[StringDataPointsByName]]]] =
    EitherDecoder.eitherDecoder[CdpApiError[CogniteId], Data[Items[StringDataPointsByName]]]
  implicit val errorOrDataPointResponseDecoder
      : Decoder[Either[CdpApiError[CogniteId], Data[Items[DataPoint]]]] =
    EitherDecoder.eitherDecoder[CdpApiError[CogniteId], Data[Items[DataPoint]]]
  implicit val errorOrStringDataPointResponseDecoder
      : Decoder[Either[CdpApiError[CogniteId], Data[Items[StringDataPoint]]]] =
    EitherDecoder.eitherDecoder[CdpApiError[CogniteId], Data[Items[StringDataPoint]]]

  def insertById(id: Long, dataPoints: Seq[DataPoint]): F[Response[Unit]] =
    request
      .post(uri"$baseUri/$id/data")
      .body(Items(dataPoints))
      .response(asJson[Either[CdpApiError[CogniteId], Unit]])
      .mapResponse {
        case Left(value) => throw value.error
        case Right(Left(cdpApiError)) => throw cdpApiError.asException(baseUri)
        case Right(Right(_)) => ()
      }
      .send()

  def insertStringsById(id: Long, dataPoints: Seq[StringDataPoint]): F[Response[Unit]] =
    request
      .post(uri"$baseUri/$id/data")
      .body(Items(dataPoints))
      .response(asJson[Either[CdpApiError[CogniteId], Unit]])
      .mapResponse {
        case Left(value) => throw value.error
        case Right(Left(cdpApiError)) => throw cdpApiError.asException(baseUri)
        case Right(Right(_)) => ()
      }
      .send()

  def deleteRangeById(id: Long, inclusiveStart: Long, exclusiveEnd: Long): F[Response[Unit]] =
    request
      .delete(
        uri"$baseUri/$id/data/deleterange".params(
          ("timestampInclusiveBegin", inclusiveStart.toString),
          ("timestampExclusiveEnd", exclusiveEnd.toString)
        )
      )
      .response(asJson[Either[CdpApiError[CogniteId], Unit]])
      .mapResponse {
        case Left(value) => throw value.error
        case Right(Left(cdpApiError)) => throw cdpApiError.asException(baseUri)
        case Right(Right(_)) => ()
      }
      .send()

  def queryById(id: Long, inclusiveStart: Long, exclusiveEnd: Long): F[Response[Seq[DataPoint]]] =
    request
      .get(
        uri"$baseUri/$id/data".params(
          ("start", inclusiveStart.toString),
          ("end", exclusiveEnd.toString)
        )
      )
      .response(asJson[Either[CdpApiError[CogniteId], Data[Items[DataPointsByName]]]])
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
      .get(
        uri"$baseUri/$id/data".params(
          ("start", inclusiveStart.toString),
          ("end", exclusiveEnd.toString)
        )
      )
      .response(asJson[Either[CdpApiError[CogniteId], Data[Items[StringDataPointsByName]]]])
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
      .get(uri"$baseUri/$id/latest")
      .response(asJson[Either[CdpApiError[CogniteId], Data[Items[DataPoint]]]])
      .mapResponse {
        case Left(value) => throw value.error
        case Right(Left(cdpApiError)) => throw cdpApiError.asException(baseUri)
        case Right(Right(value)) => extractor.extract(value).items.headOption
      }
      .send()

  def getLatestStringDataPointById(id: Long): F[Response[Option[StringDataPoint]]] =
    request
      .get(uri"$baseUri/$id/latest")
      .response(asJson[Either[CdpApiError[CogniteId], Data[Items[StringDataPoint]]]])
      .mapResponse {
        case Left(value) => throw value.error
        case Right(Left(cdpApiError)) => throw cdpApiError.asException(baseUri)
        case Right(Right(value)) => extractor.extract(value).items.headOption
      }
      .send()
}
