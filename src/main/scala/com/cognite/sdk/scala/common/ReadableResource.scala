package com.cognite.sdk.scala.common

import com.softwaremill.sttp.circe.asJson
import com.softwaremill.sttp._
//import io.circe.generic.auto._
import io.circe.Decoder

trait ReadableResource[R, F[_], C[_]] extends Resource {
  implicit val auth: Auth
  implicit val sttpBackend: SttpBackend[F, _]
  implicit val readDecoder: Decoder[R]
  implicit val containerDecoder: Decoder[C[ItemsWithCursor[R]]]
  implicit val extractor: Extractor[C]
  //implicit val extractor: Extractor[C]
  //def extract(c: C[ItemsWithCursor[R]]): ItemsWithCursor[R]

  @SuppressWarnings(Array("org.wartremover.warts.EitherProjectionPartial", "org.wartremover.warts.AsInstanceOf"))
  private def readWithCursor(cursor: Option[String]): F[Response[ItemsWithCursor[R]]] =
    sttp
      .auth(auth)
      .contentType("application/json")
      .get(cursor.fold(baseUri)(baseUri.param("cursor", _)).param("limit", defaultLimit.toString))
      .response(asJson[C[ItemsWithCursor[R]]])
      .mapResponse {
        case Left(value) => throw value.error
        case Right(value) => extractor.extract(value)
      }
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
