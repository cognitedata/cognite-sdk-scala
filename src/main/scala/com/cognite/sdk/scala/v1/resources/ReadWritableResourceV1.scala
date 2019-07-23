package com.cognite.sdk.scala.v1.resources

import com.cognite.sdk.scala.common._
import com.cognite.sdk.scala.v1._
import com.softwaremill.sttp._
import com.softwaremill.sttp.circe._
import io.circe.Decoder
import io.circe.generic.auto._

trait DeleteByExternalIdsV1[F[_]]
    extends WithRequestSession[F]
    with BaseUri
    with DeleteByExternalIds[F] {
  override def deleteByExternalIds(externalIds: Seq[String]): F[Response[Unit]] = {
    // TODO: group deletes by max deletion request size
    //       or assert that length of `ids` is less than max deletion request size
    implicit val errorOrUnitDecoder: Decoder[Either[CdpApiError, Unit]] =
      EitherDecoder.eitherDecoder[CdpApiError, Unit]
    requestSession
      .send { request =>
        request
          .post(uri"$baseUri/delete")
          .body(Items(externalIds.map(CogniteExternalId)))
          .response(asJson[Either[CdpApiError, Unit]])
          .mapResponse {
            case Left(value) => throw value.error
            case Right(Left(cdpApiError)) => throw cdpApiError.asException(uri"$baseUri/delete")
            case Right(Right(_)) => ()
          }
      }
  }
}

trait DeleteByIdsV1[R, W, F[_]]
    extends WithRequestSession[F]
    with BaseUri
    with DeleteByIds[F, Long] {
  override def deleteByIds(ids: Seq[Long]): F[Response[Unit]] = {
    implicit val errorOrUnitDecoder: Decoder[Either[CdpApiError, Unit]] =
      EitherDecoder.eitherDecoder[CdpApiError, Unit]
    // TODO: group deletes by max deletion request size
    //       or assert that length of `ids` is less than max deletion request size
    requestSession
      .send { request =>
        request
          .post(uri"$baseUri/delete")
          .body(Items(ids.map(CogniteId)))
          .response(asJson[Either[CdpApiError, Unit]])
          .mapResponse {
            case Left(value) => throw value.error
            case Right(Left(cdpApiError)) => throw cdpApiError.asException(uri"$baseUri/delete")
            case Right(Right(_)) => ()
          }
      }
  }
}
