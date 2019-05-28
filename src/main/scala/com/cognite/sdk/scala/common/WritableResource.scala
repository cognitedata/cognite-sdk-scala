package com.cognite.sdk.scala.common

import com.softwaremill.sttp.circe._
import com.softwaremill.sttp._
import io.circe.generic.auto._
import io.circe.{Decoder, Encoder}
import io.scalaland.chimney.Transformer
import io.scalaland.chimney.dsl._

trait WritableResource[R, W, F[_], C[_]] extends Resource {
  implicit val auth: Auth
  implicit val sttpBackend: SttpBackend[F, _]
  implicit val writeEncoder: Encoder[W]
  implicit val writeDecoder: Decoder[W]
  implicit val readDecoder: Decoder[R]
  implicit val containerDecoder: Decoder[C[ItemsWithCursor[R]]]
  implicit val extractor: Extractor[C]
  //def extract(c: C[ItemsWithCursor[R]]): ItemsWithCursor[R]

  @SuppressWarnings(Array("org.wartremover.warts.EitherProjectionPartial", "org.wartremover.warts.AsInstanceOf"))
  def writeItems(items: Items[W]): F[Response[Seq[R]]] =
    sttp
      .auth(auth)
      .contentType("application/json")
      .post(baseUri)
      .body(items)
      .response(asJson[C[ItemsWithCursor[R]]])
      .mapResponse {
        case Left(value) => throw value.error
        case Right(value) => extractor.extract(value).items
      }
//      .response(asJson[C[ItemsWithCursor[R]]])
//      .mapResponse {
//        case Left(value) => throw value.error
//        case Right(value) => value.extract().asInstanceOf[ItemsWithCursor[R]].items
//      }
      .send()

  def write[T](items: Seq[T])(implicit t: Transformer[T, W]): F[Response[Seq[R]]] =
    writeItems(Items(items.map(_.transformInto[W])))
}
