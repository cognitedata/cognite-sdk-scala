package com.cognite.sdk.scala.v0_6

import io.circe._
import io.circe.syntax._
import io.circe.generic.semiauto._
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

  private implicit val itemsEncoder: Encoder[Items[W]] = deriveEncoder
  private implicit val dataItemsWithCursorDecoder: Decoder[Data[ItemsWithCursor[R]]] = deriveDecoder

  val createUri: Uri

  def writeItems(items: Seq[W]): F[Response[Seq[R]]] =
    sttp
      .auth(auth)
      .contentType("application/json")
      .post(createUri)
      .body(Items(items).asJson)
      .response(asJson[Data[ItemsWithCursor[R]]])
      .mapResponse(_.right.get.data.items)
      .send()

  def write[T](items: Seq[T])(implicit t: Transformer[T, W]): F[Response[Seq[R]]] =
    writeItems(items.map(_.transformInto[W]))
}
