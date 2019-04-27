package com.cognite.sdk.scala.common

import com.cognite.sdk.scala.v0_6.{Data, ItemsWithCursor}
import com.softwaremill.sttp.circe.asJson
import com.softwaremill.sttp._
import io.circe.generic.auto._
import io.circe.Decoder

trait ReadableResource[R, F[_]] extends Resource {
  implicit val auth: Auth
  implicit val sttpBackend: SttpBackend[F, _]
  implicit val readDecoder: Decoder[R]

  @SuppressWarnings(Array("org.wartremover.warts.EitherProjectionPartial"))
  private def readWithCursor(cursor: Option[String]): F[Response[ItemsWithCursor[R]]] =
    sttp
      .auth(auth)
      .contentType("application/json")
      .get(cursor.fold(baseUri)(baseUri.param("cursor", _)).param("limit", defaultLimit.toString))
      .response(asJson[Data[ItemsWithCursor[R]]])
      .mapResponse(_.right.get.data)
      .send()

  def readFromCursor(cursor: String): F[Response[ItemsWithCursor[R]]] = readWithCursor(Some(cursor))
  def read(): F[Response[ItemsWithCursor[R]]] = readWithCursor(None)

  private def readWithNextCursor(cursor: Option[String]): Iterator[F[Seq[R]]] =
    new NextCursorIterator[R, F](cursor) {
      def get(cursor: Option[String]): F[Response[ItemsWithCursor[R]]] =
        readWithCursor(cursor)
    }

  def readAllFromCursor(cursor: String): Iterator[F[Seq[R]]] = readWithNextCursor(Some(cursor))
  def readAll(): Iterator[F[Seq[R]]] = readWithNextCursor(None)
}
