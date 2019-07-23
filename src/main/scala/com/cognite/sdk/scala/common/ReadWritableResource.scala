package com.cognite.sdk.scala.common

import com.cognite.sdk.scala.v1.CogniteExternalId
import com.softwaremill.sttp._
import com.softwaremill.sttp.circe._
import io.circe.{Decoder, Encoder}
import io.scalaland.chimney.Transformer
import io.scalaland.chimney.dsl._

trait DeleteByIds[F[_], PrimitiveId] {
  def deleteByIds(ids: Seq[PrimitiveId])(
      implicit sttpBackend: SttpBackend[F, _],
      errorDecoder: Decoder[CdpApiError],
      itemsEncoder: Encoder[Items[CogniteId]]
  ): F[Response[Unit]]
}

trait DeleteByExternalIds[F[_]] {
  def deleteByExternalIds(externalIds: Seq[String])(
      implicit sttpBackend: SttpBackend[F, _],
      errorDecoder: Decoder[CdpApiError],
      itemsEncoder: Encoder[Items[CogniteExternalId]]
  ): F[Response[Unit]]
}

trait Create[R, W, F[_]] extends WithRequestSession with BaseUri {
  def createItems(items: Items[W])(
      implicit sttpBackend: SttpBackend[F, _],
      errorDecoder: Decoder[CdpApiError],
      itemsEncoder: Encoder[Items[W]],
      itemsWithCursorDecoder: Decoder[ItemsWithCursor[R]]
  ): F[Response[Seq[R]]] = {
    implicit val errorOrStringDataPointsByIdResponseDecoder
        : Decoder[Either[CdpApiError, ItemsWithCursor[R]]] =
      EitherDecoder.eitherDecoder[CdpApiError, ItemsWithCursor[R]]
    requestSession
      .request
      .post(baseUri)
      .body(items)
      .response(asJson[Either[CdpApiError, ItemsWithCursor[R]]])
      .mapResponse {
        case Left(value) =>
          throw value.error
        case Right(Left(cdpApiError)) => throw cdpApiError.asException(baseUri)
        case Right(Right(value)) => value.items
      }
      .send()
  }

  def create[T](items: Seq[T])(
      implicit sttpBackend: SttpBackend[F, _],
      errorDecoder: Decoder[CdpApiError],
      itemsEncoder: Encoder[Items[W]],
      itemsWithCursorDecoder: Decoder[ItemsWithCursor[R]],
      t: Transformer[T, W]
  ): F[Response[Seq[R]]] =
    createItems(Items(items.map(_.transformInto[W])))
}
