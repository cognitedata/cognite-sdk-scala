package com.cognite.sdk.scala.v0_6

import com.softwaremill.sttp._
import com.softwaremill.sttp.circe._
import io.circe.Decoder
import io.circe.generic.auto._

trait ReadableResource[R, F[_]] extends Resource {
  implicit val auth: Auth
  implicit val sttpBackend: SttpBackend[F, _]
  implicit val readDecoder: Decoder[R]

  def read(cursor: Option[String]): F[Response[ItemsWithCursor[R]]] =
    sttp
      .auth(auth)
      .contentType("application/json")
      .get(cursor.fold(baseUri)(baseUri.param("cursor", _)))
      .response(asJson[Data[ItemsWithCursor[R]]])
      .mapResponse(_.right.get.data)
      .send()

  def read(): F[Response[ItemsWithCursor[R]]] = read(None)

  def readAll(cursor: Option[String]): Iterator[F[Seq[R]]] =
    new CursorIterator[R, F](cursor) {
      def get(cursor: Option[String]): F[Response[ItemsWithCursor[R]]] =
        read(cursor)
    }

  def readAll(): Iterator[F[Seq[R]]] = readAll(None)
}
