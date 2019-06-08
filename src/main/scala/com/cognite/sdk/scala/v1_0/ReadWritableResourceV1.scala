package com.cognite.sdk.scala.v1_0

import com.cognite.sdk.scala.common.{CdpApiError, CogniteId, EitherDecoder, Items, ReadWritableResource}
import com.softwaremill.sttp.circe._
import com.softwaremill.sttp._
import io.circe.Decoder
import io.circe.generic.auto._

abstract class ReadWritableResourceV1[R, W, F[_]] extends ReadWritableResource[R, W, F, Id, CogniteId] {
  def deleteByExternalIds(externalIds: Seq[String]): F[Response[Unit]] =
    request
      .post(uri"$baseUri/delete")
      .body(Items(externalIds.map(CogniteExternalId)))
      .mapResponse(_ => ())
      .send()

  implicit val errorOrUnitDecoder: Decoder[Either[CdpApiError[CogniteId], Unit]] =
    EitherDecoder.eitherDecoder[CdpApiError[CogniteId], Unit]
  def deleteByIds(ids: Seq[Long]): F[Response[Unit]] =
    // TODO: group deletes by max deletion request size
    //       or assert that length of `ids` is less than max deletion request size
    request
      .post(uri"$baseUri/delete")
      .body(Items(ids.map(CogniteId)))
      .response(asJson[Either[CdpApiError[CogniteId], Unit]])
      .mapResponse {
        case Left(value) => throw value.error
        case Right(Left(cdpApiError)) => throw cdpApiError.asException(uri"$baseUri/delete")
        case Right(Right(_)) => ()
      }
      .send()
}
