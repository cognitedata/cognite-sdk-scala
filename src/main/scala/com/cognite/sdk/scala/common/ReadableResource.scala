package com.cognite.sdk.scala.common

import com.softwaremill.sttp._
import com.softwaremill.sttp.circe._
import io.circe.generic.auto._
import io.circe.Decoder

trait ReadableResource[R, F[_], C[_]] extends Resource[F] {
  implicit val readDecoder: Decoder[R]
  implicit val containerItemsWithCursorDecoder: Decoder[C[ItemsWithCursor[R]]]
  implicit val containerItemsDecoder: Decoder[C[Items[R]]]
  implicit val extractor: Extractor[C]

  private def readWithCursor(cursor: Option[String]): F[Response[ItemsWithCursor[R]]] =
    request
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

  implicit val errorOrItemsDecoder: Decoder[Either[CdpApiError[CogniteId], C[Items[R]]]] = Decoders.eitherDecoder[CdpApiError[CogniteId], C[Items[R]]]
  def retrieveByIds(ids: Seq[Long]): F[Response[Either[Seq[CogniteId], Seq[R]]]] =
    request
      .get(uri"$baseUri/byids")
      .body(Items(ids.map(CogniteId)))
      .parseResponseIf(_ => true)
      .response(asJson[Either[CdpApiError[CogniteId], C[Items[R]]]])
      .mapResponse {
        case Left(value) => throw value.error
        case Right(value) =>
          value
            .left.map(_.error.missing.getOrElse(Seq.empty[CogniteId]))
            .right.map(extractor.extract(_).items)
      }
      .send()
}
