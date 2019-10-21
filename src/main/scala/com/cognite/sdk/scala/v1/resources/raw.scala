package com.cognite.sdk.scala.v1.resources

import cats.Applicative
import cats.implicits._
import com.cognite.sdk.scala.common._
import com.cognite.sdk.scala.v1._
import com.softwaremill.sttp._
import com.softwaremill.sttp.circe._
import io.circe.derivation.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import fs2._

object RawResource {
  def deleteByIds[F[_], I](requestSession: RequestSession[F], baseUri: Uri, ids: Seq[I])(
      implicit idsItemsEncoder: Encoder[Items[I]]
  ): F[Unit] = {
    implicit val errorOrUnitDecoder: Decoder[Either[CdpApiError, Unit]] =
      EitherDecoder.eitherDecoder[CdpApiError, Unit]
    requestSession
      .sendCdf { request =>
        request
          .post(uri"$baseUri/delete")
          .body(Items(ids))
          .response(asJson[Either[CdpApiError, Unit]])
          .mapResponse {
            case Left(value) =>
              throw value.error
            case Right(Left(cdpApiError)) => throw cdpApiError.asException(uri"$baseUri/delete")
            case Right(Right(_)) => ()
          }
      }
  }
}

class RawDatabases[F[_]](val requestSession: RequestSession[F])
    extends WithRequestSession[F]
    with Readable[RawDatabase, F]
    with Create[RawDatabase, RawDatabase, F]
    with DeleteByIds[F, String] {
  import RawDatabases._
  override val baseUri = uri"${requestSession.baseUri}/raw/dbs"

  override private[sdk] def readWithCursor(
      cursor: Option[String],
      limit: Option[Int],
      partition: Option[Partition]
  ): F[ItemsWithCursor[RawDatabase]] =
    Readable.readWithCursor(
      requestSession,
      baseUri,
      cursor,
      limit,
      None,
      Constants.defaultBatchSize
    )

  override def createItems(items: Items[RawDatabase]): F[Seq[RawDatabase]] =
    Create.createItems[F, RawDatabase, RawDatabase](requestSession, baseUri, items)

  override def deleteByIds(ids: Seq[String]): F[Unit] =
    RawResource.deleteByIds(requestSession, baseUri, ids.map(RawDatabase))
}

object RawDatabases {
  implicit val rawDatabaseItemsWithCursorDecoder: Decoder[ItemsWithCursor[RawDatabase]] =
    deriveDecoder[ItemsWithCursor[RawDatabase]]
  implicit val rawDatabaseItemsDecoder: Decoder[Items[RawDatabase]] =
    deriveDecoder[Items[RawDatabase]]
  implicit val rawDatabaseItemsEncoder: Encoder[Items[RawDatabase]] =
    deriveEncoder[Items[RawDatabase]]
  implicit val rawDatabaseEncoder: Encoder[RawDatabase] = deriveEncoder[RawDatabase]
  implicit val rawDatabaseDecoder: Decoder[RawDatabase] = deriveDecoder[RawDatabase]
}

class RawTables[F[_]](val requestSession: RequestSession[F], database: String)
    extends WithRequestSession[F]
    with Readable[RawTable, F]
    with Create[RawTable, RawTable, F]
    with DeleteByIds[F, String] {
  import RawTables._
  override val baseUri =
    uri"${requestSession.baseUri}/raw/dbs/$database/tables"

  override private[sdk] def readWithCursor(
      cursor: Option[String],
      limit: Option[Int],
      partition: Option[Partition]
  ): F[ItemsWithCursor[RawTable]] =
    Readable.readWithCursor(
      requestSession,
      baseUri,
      cursor,
      limit,
      None,
      Constants.defaultBatchSize
    )

  override def createItems(items: Items[RawTable]): F[Seq[RawTable]] =
    Create.createItems[F, RawTable, RawTable](requestSession, baseUri, items)

  override def deleteByIds(ids: Seq[String]): F[Unit] =
    RawResource.deleteByIds(requestSession, baseUri, ids.map(RawTable))
}

object RawTables {
  implicit val rawTableItemsWithCursorDecoder: Decoder[ItemsWithCursor[RawTable]] =
    deriveDecoder[ItemsWithCursor[RawTable]]
  implicit val rawTableItemsDecoder: Decoder[Items[RawTable]] =
    deriveDecoder[Items[RawTable]]
  implicit val rawTableItemsEncoder: Encoder[Items[RawTable]] =
    deriveEncoder[Items[RawTable]]
  implicit val rawTableEncoder: Encoder[RawTable] = deriveEncoder[RawTable]
  implicit val rawTableDecoder: Decoder[RawTable] = deriveDecoder[RawTable]
}

class RawRows[F[_]](val requestSession: RequestSession[F], database: String, table: String)
    extends WithRequestSession[F]
    with Readable[RawRow, F]
    with Create[RawRow, RawRow, F]
    with DeleteByIds[F, String]
    with PartitionedFilterF[RawRow, RawRowFilter, F] {
  implicit val stringItemsDecoder: Decoder[Items[String]] = deriveDecoder[Items[String]]

  implicit val errorOrStringItemsDecoder: Decoder[Either[CdpApiError, Items[String]]] =
    EitherDecoder.eitherDecoder[CdpApiError, Items[String]]

  import RawRows._
  override val baseUri =
    uri"${requestSession.baseUri}/raw/dbs/$database/tables/$table/rows"

  val cursorsUri = uri"${requestSession.baseUri}/raw/dbs/$database/tables/$table/cursors"

  // raw does not return the created rows in the response, so we'll always return an empty sequence.
  override def createItems(items: Items[RawRow]): F[Seq[RawRow]] = {
    implicit val errorOrUnitDecoder: Decoder[Either[CdpApiError, Unit]] =
      EitherDecoder.eitherDecoder[CdpApiError, Unit]
    requestSession
      .sendCdf { request =>
        request
          .post(baseUri)
          .body(items)
          .response(asJson[Either[CdpApiError, Unit]])
          .mapResponse {
            case Left(value) => throw value.error
            case Right(Left(cdpApiError)) => throw cdpApiError.asException(uri"$baseUri/byids")
            case Right(Right(_)) => Seq.empty[RawRow]
          }
      }
  }

  override private[sdk] def readWithCursor(
      cursor: Option[String],
      limit: Option[Int],
      partition: Option[Partition]
  ): F[ItemsWithCursor[RawRow]] =
    Readable.readWithCursor(requestSession, baseUri, cursor, limit, None, Constants.rowsBatchSize)

  override def deleteByIds(ids: Seq[String]): F[Unit] =
    RawResource.deleteByIds(requestSession, baseUri, ids.map(RawRowKey))

  override private[sdk] def filterWithCursor(
      filter: RawRowFilter,
      cursor: Option[String],
      limit: Option[Int],
      partition: Option[Partition]
  ): F[ItemsWithCursor[RawRow]] =
    Readable.readWithCursor(
      requestSession,
      baseUri.params(filterToParams(filter)),
      cursor,
      limit,
      None,
      Constants.defaultBatchSize
    )

  override def filterPartitionsF(
      filter: RawRowFilter,
      numPartitions: Int,
      limitPerPartition: Option[Int]
  )(implicit F: Applicative[F]): F[Seq[Stream[F, RawRow]]] = {
    val cursorsUriWithParams = cursorsUri
      .param("numberOfCursors", numPartitions.toString)
      .params(lastUpdatedTimeFilterToParams(filter))

    for {
      cursors <- requestSession
        .sendCdf { request =>
          request
            .get(cursorsUriWithParams)
            .response(asJson[Either[CdpApiError, Items[String]]])
            .mapResponse {
              case Left(value) => throw value.error
              case Right(Left(cdpApiError)) =>
                throw cdpApiError.asException(cursorsUriWithParams)
              case Right(Right(value)) => value
            }
        }
      streams = cursors.items.map(
        cursor =>
          Readable
            .pullFromCursor(
              Some(cursor),
              limitPerPartition,
              None,
              filterWithCursor(filter, _, _, _)
            )
            .stream
      )
    } yield streams
  }

  def filterToParams(filter: RawRowFilter): Map[String, String] =
    Map(
      "columns" -> filter.columns.map { columns =>
        if (columns.isEmpty) {
          ","
        } else {
          columns.mkString(",")
        }
      }
    ).collect {
      case (key, Some(value)) => key -> value
    } ++ lastUpdatedTimeFilterToParams(filter)

  def lastUpdatedTimeFilterToParams(filter: RawRowFilter): Map[String, String] =
    Map(
      "minLastUpdatedTime" -> filter.minLastUpdatedTime.map(_.toEpochMilli.toString),
      "maxLastUpdatedTime" -> filter.maxLastUpdatedTime.map(_.toEpochMilli.toString)
    ).collect {
      case (key, Some(value)) => key -> value
    }
}

object RawRows {
  implicit val rawRowEncoder: Encoder[RawRow] = deriveEncoder[RawRow]
  implicit val rawRowDecoder: Decoder[RawRow] = deriveDecoder[RawRow]
  implicit val rawRowItemsWithCursorDecoder: Decoder[ItemsWithCursor[RawRow]] =
    deriveDecoder[ItemsWithCursor[RawRow]]
  implicit val rawRowItemsDecoder: Decoder[Items[RawRow]] =
    deriveDecoder[Items[RawRow]]
  implicit val rawRowItemsEncoder: Encoder[Items[RawRow]] =
    deriveEncoder[Items[RawRow]]

  implicit val rawRowKeyEncoder: Encoder[RawRowKey] = deriveEncoder[RawRowKey]
  implicit val rawRowKeyItemsEncoder: Encoder[Items[RawRowKey]] =
    deriveEncoder[Items[RawRowKey]]
}
