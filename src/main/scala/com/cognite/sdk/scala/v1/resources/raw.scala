package com.cognite.sdk.scala.v1.resources

import com.cognite.sdk.scala.common._
import com.cognite.sdk.scala.v1.{RawDatabase, RawRow, RawRowKey, RawTable}
import com.softwaremill.sttp._
import com.softwaremill.sttp.circe._
import io.circe.generic.auto._
import io.circe.generic.semiauto.deriveEncoder
import io.circe.{Decoder, Encoder}

abstract class RawResource[R: Decoder, W: Decoder: Encoder, F[_], InternalId: Encoder](
    implicit auth: Auth
) extends WithRequestSession
    with Readable[R, F, Id]
    with Create[R, W, F, Id]
    with DeleteByIds[F, String] {
  def toInternalId(id: String): InternalId
  implicit val errorOrUnitDecoder: Decoder[Either[CdpApiError, Unit]] =
    EitherDecoder.eitherDecoder[CdpApiError, Unit]
  override def deleteByIds(ids: Seq[String])(
      implicit sttpBackend: SttpBackend[F, _],
      auth: Auth,
      errorDecoder: Decoder[CdpApiError],
      itemsEncoder: Encoder[Items[CogniteId]]
  ): F[Response[Unit]] =
    requestSession
      .request
      .post(uri"$baseUri/delete")
      .body(Items(ids.map(toInternalId)))
      .response(asJson[Either[CdpApiError, Unit]])
      .mapResponse {
        case Left(value) =>
          throw value.error
        case Right(Left(cdpApiError)) => throw cdpApiError.asException(uri"$baseUri/delete")
        case Right(Right(_)) => ()
      }
      .send()
}

class RawDatabases[F[_]](val requestSession: RequestSession)
    extends RawResource[RawDatabase, RawDatabase, F, RawDatabase] {
  def toInternalId(id: String): RawDatabase = RawDatabase(id)
  implicit val idEncoder: Encoder[RawDatabase] = deriveEncoder
  override val baseUri = uri"${requestSession.baseUri}/raw/dbs"
}

class RawTables[F[_]](val requestSession: RequestSession, database: String)(
    implicit auth: Auth
) extends RawResource[RawTable, RawTable, F, RawTable] {
  def toInternalId(id: String): RawTable = RawTable(id)
  implicit val idEncoder: Encoder[RawTable] = deriveEncoder
  override val baseUri =
    uri"${requestSession.baseUri}/raw/dbs/$database/tables"
}

class RawRows[F[_]](val requestSession: RequestSession, database: String, table: String)(
    implicit auth: Auth
) extends RawResource[RawRow, RawRow, F, RawRowKey] {
  def toInternalId(id: String): RawRowKey = RawRowKey(id)
  implicit val idEncoder: Encoder[RawRowKey] = deriveEncoder
  override val baseUri =
    uri"${requestSession.baseUri}/raw/dbs/$database/tables/$table/rows"

  // raw does not return the created rows in the response, so we'll always return an empty sequence.
  override def createItems(
      items: Items[RawRow]
  )(
      implicit sttpBackend: SttpBackend[F, _],
      auth: Auth,
      extractor: Extractor[Id],
      errorDecoder: Decoder[CdpApiError],
      itemsEncoder: Encoder[Items[RawRow]],
      itemsWithCursorDecoder: Decoder[Id[ItemsWithCursor[RawRow]]]
  ): F[Response[Seq[RawRow]]] =
    requestSession
      .request
      .post(baseUri)
      .body(items)
      .response(asJson[Either[CdpApiError, Unit]])
      .mapResponse {
        case Left(value) => throw value.error
        case Right(Left(cdpApiError)) => throw cdpApiError.asException(uri"$baseUri/byids")
        case Right(Right(_)) => Seq.empty[RawRow]
      }
      .send()
}
