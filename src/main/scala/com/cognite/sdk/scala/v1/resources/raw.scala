package com.cognite.sdk.scala.v1.resources

import com.cognite.sdk.scala.common._
import com.cognite.sdk.scala.v1._
import com.softwaremill.sttp._
import com.softwaremill.sttp.circe._
import io.circe.generic.semiauto.deriveEncoder
import io.circe.{Decoder, Encoder}

abstract class RawResource[R: Decoder, W: Decoder: Encoder, F[_], InternalId: Encoder](
    implicit auth: Auth
) extends WithRequestSession
    with Readable[R, F]
    with Create[R, W, F]
    with DeleteByIds[F, String] {
  def toInternalId(id: String): InternalId

  implicit val internalIdItemsEncoder: Encoder[Items[InternalId]] = deriveEncoder[Items[InternalId]]
  override def deleteByIds(ids: Seq[String])(
      implicit sttpBackend: SttpBackend[F, _]
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

class RawTables[F[_]](val requestSession: RequestSession, database: String)
    extends RawResource[RawTable, RawTable, F, RawTable] {
  def toInternalId(id: String): RawTable = RawTable(id)
  implicit val idEncoder: Encoder[RawTable] = deriveEncoder
  override val baseUri =
    uri"${requestSession.baseUri}/raw/dbs/$database/tables"
}

class RawRows[F[_]](val requestSession: RequestSession, database: String, table: String)
    extends RawResource[RawRow, RawRow, F, RawRowKey] {
  def toInternalId(id: String): RawRowKey = RawRowKey(id)
  implicit val idEncoder: Encoder[RawRowKey] = deriveEncoder
  override val baseUri =
    uri"${requestSession.baseUri}/raw/dbs/$database/tables/$table/rows"

  implicit val errorOrUnitDecoder: Decoder[Either[CdpApiError, Unit]] =
    EitherDecoder.eitherDecoder[CdpApiError, Unit]

  private implicit val rawRowItemsEncoder = deriveEncoder[Items[RawRow]]
  // raw does not return the created rows in the response, so we'll always return an empty sequence.
  override def createItems(
      items: Items[RawRow]
  )(
      implicit sttpBackend: SttpBackend[F, _],
      readDecoder: Decoder[ItemsWithCursor[RawRow]],
      itemsEncoder: Encoder[Items[RawRow]]
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
