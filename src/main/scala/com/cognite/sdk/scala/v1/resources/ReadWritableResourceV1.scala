package com.cognite.sdk.scala.v1.resources

import com.cognite.sdk.scala.common._
import com.cognite.sdk.scala.v1.CogniteExternalId
import com.softwaremill.sttp._
import com.softwaremill.sttp.circe._
import io.circe.{Decoder, Encoder}

abstract class ReadWritableResourceV1[R: Decoder, W: Decoder: Encoder, F[_]](
    implicit auth: Auth
) extends ReadWritableResourceWithRetrieve[R, W, F, Id, CogniteId, Long]
    with DeleteByIdsV1[R, W, F, Id, CogniteId, Long]
    with DeleteByExternalIdsV1[F] {}

trait DeleteByExternalIdsV1[F[_]]
    extends RequestSession
    with BaseUri
    with DeleteByExternalIds[F, CogniteId, CogniteExternalId] {
  override def deleteByExternalIds(externalIds: Seq[String])(
      implicit sttpBackend: SttpBackend[F, _],
      auth: Auth,
      errorDecoder: Decoder[CdpApiError],
      itemsEncoder: Encoder[Items[CogniteExternalId]]
  ): F[Response[Unit]] = {
    implicit val errorOrUnitDecoder: Decoder[Either[CdpApiError, Unit]] =
      EitherDecoder.eitherDecoder[CdpApiError, Unit]
    // TODO: group deletes by max deletion request size
    //       or assert that length of `ids` is less than max deletion request size
    request
      .post(uri"$baseUri/delete")
      .body(Items(externalIds.map(CogniteExternalId)))
      .response(asJson[Either[CdpApiError, Unit]])
      .mapResponse {
        case Left(value) => throw value.error
        case Right(Left(cdpApiError)) => throw cdpApiError.asException(uri"$baseUri/delete")
        case Right(Right(_)) => ()
      }
      .send()
  }
}

trait DeleteByIdsV1[R, W, F[_], C[_], InternalId, PrimitiveId]
    extends RequestSession
    with ToInternalId[InternalId, PrimitiveId]
    with BaseUri
    with DeleteByIds[F, InternalId, PrimitiveId] {
  override def deleteByIds(ids: Seq[PrimitiveId])(
      implicit sttpBackend: SttpBackend[F, _],
      auth: Auth,
      errorDecoder: Decoder[CdpApiError],
      itemsEncoder: Encoder[Items[InternalId]]
  ): F[Response[Unit]] = {
    implicit val errorOrUnitDecoder: Decoder[Either[CdpApiError, Unit]] =
      EitherDecoder.eitherDecoder[CdpApiError, Unit]
    // TODO: group deletes by max deletion request size
    //       or assert that length of `ids` is less than max deletion request size
    request
      .post(uri"$baseUri/delete")
      .body(Items(ids.map(toInternalId)))
      .response(asJson[Either[CdpApiError, Unit]])
      .mapResponse {
        case Left(value) => throw value.error
        case Right(Left(cdpApiError)) => throw cdpApiError.asException(uri"$baseUri/delete")
        case Right(Right(_)) => ()
      }
      .send()
  }
}
