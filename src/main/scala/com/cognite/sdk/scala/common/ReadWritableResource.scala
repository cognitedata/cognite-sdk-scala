package com.cognite.sdk.scala.common

import com.softwaremill.sttp._
import com.softwaremill.sttp.circe._
import io.circe.generic.auto._
import io.circe.{Decoder, Encoder}
import io.scalaland.chimney.Transformer
import io.scalaland.chimney.dsl._

abstract class ReadWritableResource[R: Decoder, W: Decoder : Encoder, F[_], C[_], I](
  implicit auth: Auth,
  sttpBackend: SttpBackend[F, _],
  containerItemsDecoder: Decoder[C[Items[R]]],
  containerItemsWithCursorDecoder: Decoder[C[ItemsWithCursor[R]]]
  ) extends ReadableResource[R, F, C, I] {
  implicit val extractor: Extractor[C]

  implicit val errorOrStringDataPointsByIdResponseDecoder
      : Decoder[Either[CdpApiError[CogniteId], C[ItemsWithCursor[R]]]] =
    EitherDecoder.eitherDecoder[CdpApiError[CogniteId], C[ItemsWithCursor[R]]]
  def createItems(items: Items[W]): F[Response[Seq[R]]] =
    request
      .post(baseUri)
      .body(items)
      .response(asJson[Either[CdpApiError[CogniteId], C[ItemsWithCursor[R]]]])
      .mapResponse {
        case Left(value) =>
          throw value.error
        case Right(Left(cdpApiError)) => throw cdpApiError.asException(baseUri)
        case Right(Right(value)) => extractor.extract(value).items
      }
      .send()

  def create[T](items: Seq[T])(implicit t: Transformer[T, W]): F[Response[Seq[R]]] =
    createItems(Items(items.map(_.transformInto[W])))

  def deleteByIds(ids: Seq[Long]): F[Response[Unit]]
}
