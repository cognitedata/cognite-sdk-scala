package com.cognite.sdk.scala.v06.resources

import com.cognite.sdk.scala.common._
import com.cognite.sdk.scala.v06.Data
import com.softwaremill.sttp._
import com.softwaremill.sttp.circe._
import io.circe.{Decoder, Encoder}

abstract class ReadWritableResourceV0_6[R: Decoder, W: Decoder: Encoder, F[_]](
    implicit auth: Auth
) extends ReadWritableResourceWithRetrieve[R, W, F, Data, Long, Long]
    with DeleteByIdsV0_6[F] {}

trait DeleteByIdsV0_6[F[_]]
    extends RequestSession
    with ToInternalId[Long, Long]
    with BaseUri
    with DeleteByIds[F, Long, Long] {
  override def deleteByIds(ids: Seq[Long])(
      implicit sttpBackend: SttpBackend[F, _],
      auth: Auth,
      errorDecoder: Decoder[CdpApiError[CogniteId]],
      itemsEncoder: Encoder[Items[Long]]
  ): F[Response[Unit]] = {
    implicit val errorOrUnitDecoder: Decoder[Either[CdpApiError[CogniteId], Unit]] =
      EitherDecoder.eitherDecoder[CdpApiError[CogniteId], Unit]
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

}
