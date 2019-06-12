package com.cognite.sdk.scala.v1_0

import com.cognite.sdk.scala.common.{
  Auth,
  CdpApiError,
  CogniteId,
  EitherDecoder,
  Extractor,
  ExtractorInstances,
  Items,
  ReadWritableResource,
  WithId
}
import com.softwaremill.sttp._
import com.softwaremill.sttp.circe._
import io.circe.{Decoder, Encoder, Json}
import io.circe.generic.auto._
import io.circe.generic.semiauto.deriveEncoder

final case class RawDatabase(name: String) extends WithId[String] {
  override val id: String = this.name
}
final case class RawRow(
    key: String,
    columns: Map[String, Json],
    lastUpdatedTime: Option[Long] = None
)
final case class RawRowKey(key: String)

abstract class RawResource[R: Decoder, W: Decoder: Encoder, F[_], InternalId, PrimitiveId](
    implicit auth: Auth,
    sttpBackend: SttpBackend[F, _]
) extends ReadWritableResource[R, W, F, Id, InternalId, PrimitiveId] {
  implicit val errorOrUnitDecoder: Decoder[Either[CdpApiError[CogniteId], Unit]] =
    EitherDecoder.eitherDecoder[CdpApiError[CogniteId], Unit]
  override def deleteByIds(ids: Seq[PrimitiveId]): F[Response[Unit]] =
    request
      .post(uri"$baseUri/delete")
      .body(Items(ids.map(toInternalId)))
      .response(asJson[Either[CdpApiError[CogniteId], Unit]])
      .mapResponse {
        case Left(value) =>
          throw value.error
        case Right(Left(cdpApiError)) => throw cdpApiError.asException(uri"$baseUri/delete")
        case Right(Right(_)) => ()
      }
      .send()
}

class RawDatabases[F[_]](project: String)(implicit auth: Auth, sttpBackend: SttpBackend[F, _])
    extends RawResource[RawDatabase, RawDatabase, F, RawDatabase, String] {
  def toInternalId(id: String): RawDatabase = RawDatabase(id)
  implicit val extractor: Extractor[Id] = ExtractorInstances.idExtractor
  implicit val idEncoder: Encoder[RawDatabase] = deriveEncoder
  override val baseUri = uri"https://api.cognitedata.com/api/v1/projects/$project/raw/dbs"
}

final case class RawTable(name: String) extends WithId[String] {
  override val id: String = this.name
}

class RawTables[F[_]](project: String, database: String)(implicit auth: Auth, sttpBackend: SttpBackend[F, _])
    extends RawResource[RawTable, RawTable, F, RawTable, String] {
  def toInternalId(id: String): RawTable = RawTable(id)
  implicit val extractor: Extractor[Id] = ExtractorInstances.idExtractor
  implicit val idEncoder: Encoder[RawTable] = deriveEncoder
  override val baseUri =
    uri"https://api.cognitedata.com/api/v1/projects/$project/raw/dbs/$database/tables"
}

class RawRows[F[_]](project: String, database: String, table: String)(
    implicit auth: Auth,
    sttpBackend: SttpBackend[F, _]
) extends RawResource[RawRow, RawRow, F, RawRowKey, String] {
  def toInternalId(id: String): RawRowKey = RawRowKey(id)
  implicit val extractor: Extractor[Id] = ExtractorInstances.idExtractor
  implicit val idEncoder: Encoder[RawRowKey] = deriveEncoder
  override val baseUri =
    uri"https://api.cognitedata.com/api/v1/projects/$project/raw/dbs/$database/tables/$table/rows"

  // raw does not return the created rows in the response, so we'll always return an empty sequence.
  override def createItems(items: Items[RawRow]): F[Response[Seq[RawRow]]] =
    request
      .post(baseUri)
      .body(items)
      .response(asJson[Either[CdpApiError[CogniteId], Unit]])
      .mapResponse {
        case Left(value) => throw value.error
        case Right(Left(cdpApiError)) => throw cdpApiError.asException(uri"$baseUri/byids")
        case Right(Right(_)) => Seq.empty[RawRow]
      }
      .send()
}
