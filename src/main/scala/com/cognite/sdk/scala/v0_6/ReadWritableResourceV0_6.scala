package com.cognite.sdk.scala.v0_6

import com.cognite.sdk.scala.common._
import com.softwaremill.sttp._
import com.softwaremill.sttp.circe._
import io.circe.Decoder
import io.circe.generic.auto._

abstract class ReadWritableResourceV0_6[R, W, F[_]] extends ReadWritableResource[R, W, F, Data, Long] {
  implicit val errorOrUnitDecoder: Decoder[Either[CdpApiError[CogniteId], Unit]] =
    EitherDecoder.eitherDecoder[CdpApiError[CogniteId], Unit]
  def deleteByIds(ids: Seq[Long]): F[Response[Unit]] =
    // TODO: group deletes by max deletion request size
    //       or assert that length of `ids` is less than max deletion request size
    request
      .post(uri"$baseUri/delete")
      .body(Items(ids))
      .response(asJson[Either[CdpApiError[CogniteId], Unit]])
      .mapResponse {
        case Left(value) =>
          throw value.error
        case Right(Left(cdpApiError)) => throw cdpApiError.asException(uri"$baseUri/delete")
        case Right(Right(_)) => ()
      }
      .send()
}
