package com.cognite.sdk.scala.v0_6

import io.circe.{Decoder, Encoder}
import io.circe.generic.auto._
import com.softwaremill.sttp._
import com.softwaremill.sttp.circe._
import io.scalaland.chimney.Transformer
import io.scalaland.chimney.dsl._

trait WritableResource[R, W, F[_]] {
  implicit val auth: Auth
  implicit val sttpBackend: SttpBackend[F, _]
  implicit val writeEncoder: Encoder[W]
  implicit val writeDecoder: Decoder[W]
  implicit val readDecoder: Decoder[R]

  val createUri: Uri

  def writeItems(items: Items[W]): F[Response[Seq[R]]] =
    sttp
      .auth(auth)
      .contentType("application/json")
      .post(createUri)
      .body(items)
      .response(asJson[Data[ItemsWithCursor[R]]])
      .mapResponse(_.right.get.data.items)
      .send()

  def write[T](items: Seq[T])(implicit t: Transformer[T, W]): F[Response[Seq[R]]] =
    writeItems(Items(items.map(_.transformInto[W])))
}
