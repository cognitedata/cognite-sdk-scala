package com.cognite.sdk.scala.v0_6

import com.softwaremill.sttp._
import com.softwaremill.sttp.circe._
import io.circe._
import io.circe.generic.semiauto.deriveDecoder
import io.circe.generic.auto._

trait ReadableResource[R, F[_]] {
  implicit val auth: Auth
  implicit val sttpBackend: SttpBackend[F, _]
  implicit val readDecoder: Decoder[R]

  private implicit val dataItemsWithCursorDecoder: Decoder[Data[ItemsWithCursor[R]]] = deriveDecoder

  val listUri: Uri

  def read(cursor: Option[String] = None): F[Response[ItemsWithCursor[R]]] =
    sttp
      .auth(auth)
      .contentType("application/json")
      .get(cursor.fold(listUri)(listUri.param("cursor", _)).param("limit", "10"))
      .response(asJson[Data[ItemsWithCursor[R]]])
      .mapResponse(_.right.get.data)
      .send()

  def readAll(cursor: Option[String] = None): Iterator[F[Seq[R]]] =
    new CursorIterator[R, F](cursor) {
      def get(cursor: Option[String]): F[Response[ItemsWithCursor[R]]] =
        read(cursor)
    }
}
