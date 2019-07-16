package com.cognite.sdk.scala.v1.resources

import com.cognite.sdk.scala.common._
import com.cognite.sdk.scala.v1.{RawDatabase, RawRow, RawRowKey, RawTable}
import com.softwaremill.sttp._
import com.softwaremill.sttp.circe._
import io.circe.generic.auto._
import io.circe.generic.semiauto.deriveEncoder
import io.circe.{Decoder, Encoder}

abstract class RawResource[R: Decoder, W: Decoder: Encoder, F[_], InternalId: Encoder, PrimitiveId](
    implicit auth: Auth
) extends ReadWritableResourceWithRetrieve[R, W, F, Id, InternalId, PrimitiveId]
    with DeleteByIdsV1[R, W, F, Id, InternalId, PrimitiveId] {
  implicit val errorOrUnitDecoder: Decoder[Either[CdpApiError, Unit]] =
    EitherDecoder.eitherDecoder[CdpApiError, Unit]
  override def deleteByIds(ids: Seq[PrimitiveId])(
      implicit sttpBackend: SttpBackend[F, _],
      auth: Auth,
      errorDecoder: Decoder[CdpApiError],
      itemsEncoder: Encoder[Items[InternalId]]
  ): F[Response[Unit]] =
    request
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

class RawDatabases[F[_]](project: String)(implicit auth: Auth)
    extends RawResource[RawDatabase, RawDatabase, F, RawDatabase, String] {
  def toInternalId(id: String): RawDatabase = RawDatabase(id)
  implicit val idEncoder: Encoder[RawDatabase] = deriveEncoder
  override val baseUri = uri"https://api.cognitedata.com/api/v1/projects/$project/raw/dbs"
}

class RawTables[F[_]](project: String, database: String)(
    implicit auth: Auth
) extends RawResource[RawTable, RawTable, F, RawTable, String] {
  def toInternalId(id: String): RawTable = RawTable(id)
  implicit val idEncoder: Encoder[RawTable] = deriveEncoder
  override val baseUri =
    uri"https://api.cognitedata.com/api/v1/projects/$project/raw/dbs/$database/tables"
}

class RawRows[F[_]](project: String, database: String, table: String)(
    implicit auth: Auth
) extends RawResource[RawRow, RawRow, F, RawRowKey, String] {
  def toInternalId(id: String): RawRowKey = RawRowKey(id)
  implicit val idEncoder: Encoder[RawRowKey] = deriveEncoder
  override val baseUri =
    uri"https://api.cognitedata.com/api/v1/projects/$project/raw/dbs/$database/tables/$table/rows"

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
    request
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
