package com.cognite.sdk.scala.v0_6

import com.cognite.sdk.scala.common._
import com.softwaremill.sttp._
import com.softwaremill.sttp.circe._
import io.circe.Decoder
import io.circe.generic.auto._

trait WritableResourceV0_6[R, W, F[_]] extends WritableResource[R, W, F, Data, Long] {
  implicit val errorOrUnitDecoder: Decoder[Either[CdpApiError[Map[String, String]], Unit]] =
    EitherDecoder.eitherDecoder[CdpApiError[Map[String, String]], Unit]
  def deleteByIds(ids: Seq[Long]): F[Response[Unit]] =
    // TODO: group deletes by max deletion request size
    //       or assert that length of `ids` is less than max deletion request size
    request
      .post(uri"$baseUri/delete")
      .body(Items(ids))
      .response(asJson[Either[CdpApiError[Map[String, String]], Unit]])
      .mapResponse {
        case Left(value) =>
          throw value.error
        case Right(Left(cdpApiError)) => throw cdpApiError.asException(uri"$baseUri/delete")
        case Right(Right(_)) => ()
      }
      .send()
}
