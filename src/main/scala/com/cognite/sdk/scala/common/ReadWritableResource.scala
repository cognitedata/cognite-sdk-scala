package com.cognite.sdk.scala.common

import com.cognite.sdk.scala.v1.CogniteExternalId
import com.softwaremill.sttp._
import com.softwaremill.sttp.circe._
import io.circe.{Decoder, Encoder}
import io.scalaland.chimney.Transformer
import io.scalaland.chimney.dsl._

//abstract class ReadWritableResource[R: Decoder, W: Decoder: Encoder, F[_], C[_], InternalId, PrimitiveId](
//    implicit auth: Auth
//) extends ReadableResource[R, F, C, InternalId, PrimitiveId]
//    with Create[R, W, F, C, InternalId, PrimitiveId]
//    with DeleteByIds[F] {}

trait DeleteByIds[F[_]] {
  def deleteByIds(ids: Seq[Long])(
      implicit sttpBackend: SttpBackend[F, _],
      auth: Auth,
      errorDecoder: Decoder[CdpApiError],
      itemsEncoder: Encoder[Items[CogniteId]]
  ): F[Response[Unit]]
}

trait DeleteByExternalIds[F[_]] {
  def deleteByExternalIds(externalIds: Seq[String])(
      implicit sttpBackend: SttpBackend[F, _],
      auth: Auth,
      errorDecoder: Decoder[CdpApiError],
      itemsEncoder: Encoder[Items[CogniteExternalId]]
  ): F[Response[Unit]]
}

trait Create[R, W, F[_], C[_]] extends WithRequestSession with BaseUri {
  def createItems(items: Items[W])(
      implicit sttpBackend: SttpBackend[F, _],
      auth: Auth,
      extractor: Extractor[C],
      errorDecoder: Decoder[CdpApiError],
      itemsEncoder: Encoder[Items[W]],
      itemsWithCursorDecoder: Decoder[C[ItemsWithCursor[R]]]
  ): F[Response[Seq[R]]] = {
    implicit val errorOrStringDataPointsByIdResponseDecoder
        : Decoder[Either[CdpApiError, C[ItemsWithCursor[R]]]] =
      EitherDecoder.eitherDecoder[CdpApiError, C[ItemsWithCursor[R]]]
    requestSession
      .request
      .post(baseUri)
      .body(items)
      .response(asJson[Either[CdpApiError, C[ItemsWithCursor[R]]]])
      .mapResponse {
        case Left(value) =>
          throw value.error
        case Right(Left(cdpApiError)) => throw cdpApiError.asException(baseUri)
        case Right(Right(value)) => extractor.extract(value).items
      }
      .send()
  }

  def create[T](items: Seq[T])(
      implicit sttpBackend: SttpBackend[F, _],
      auth: Auth,
      extractor: Extractor[C],
      errorDecoder: Decoder[CdpApiError],
      itemsEncoder: Encoder[Items[W]],
      itemsWithCursorDecoder: Decoder[C[ItemsWithCursor[R]]],
      t: Transformer[T, W]
  ): F[Response[Seq[R]]] =
    createItems(Items(items.map(_.transformInto[W])))
}
