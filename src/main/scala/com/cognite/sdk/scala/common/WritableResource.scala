package com.cognite.sdk.scala.common

import com.softwaremill.sttp._
import com.softwaremill.sttp.circe._
import io.circe.generic.auto._
import io.circe.{Decoder, Encoder}
import io.scalaland.chimney.Transformer
import io.scalaland.chimney.dsl._

trait WritableResource[R, W, F[_], C[_]] extends Resource[F] {
  implicit val writeEncoder: Encoder[W]
  implicit val writeDecoder: Decoder[W]
  implicit val readDecoder: Decoder[R]
  implicit val containerItemsWithCursorDecoder: Decoder[C[ItemsWithCursor[R]]]
  implicit val extractor: Extractor[C]

  def createItems(items: Items[W]): F[Response[Seq[R]]] =
    request
      .post(baseUri)
      .body(items)
      .response(asJson[C[ItemsWithCursor[R]]])
      .mapResponse {
        case Left(value) => throw value.error
        case Right(value) => extractor.extract(value).items
      }
      .send()

  def create[T](items: Seq[T])(implicit t: Transformer[T, W]): F[Response[Seq[R]]] =
    createItems(Items(items.map(_.transformInto[W])))

  implicit val errorOrUnitDecoder: Decoder[Either[CdpApiError[CogniteId], Unit]] =
    Decoders.eitherDecoder[CdpApiError[CogniteId], Unit]
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
