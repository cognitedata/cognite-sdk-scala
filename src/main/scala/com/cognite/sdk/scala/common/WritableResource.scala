package com.cognite.sdk.scala.common

import com.softwaremill.sttp.circe._
import com.softwaremill.sttp._
import io.circe.generic.auto._
import io.circe.{Decoder, Encoder}
import io.scalaland.chimney.Transformer
import io.scalaland.chimney.dsl._

trait WritableResource[R, W, F[_], C[_]] extends Resource[F] {
  implicit val writeEncoder: Encoder[W]
  implicit val writeDecoder: Decoder[W]
  implicit val readDecoder: Decoder[R]
  implicit val containerDecoder: Decoder[C[ItemsWithCursor[R]]]
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

  def deleteByIds(ids: Seq[Long]): F[Response[Unit]] =
    // TODO: group deletes by max deletion request size
    request
      .post(uri"$baseUri/delete")
      .body(Items(ids.map(CogniteId)))
      .mapResponse(_ => ())
      .send()
}
