// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1.resources

import cats.Applicative
import cats.implicits._
import com.cognite.sdk.scala.common._
import com.cognite.sdk.scala.v1._
import sttp.client3._
import sttp.client3.circe._
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import fs2._
import sttp.model.Uri

object RawResource {
  def deleteByIds[F[_], I](requestSession: RequestSession[F], baseUrl: Uri, ids: Seq[I])(
      implicit idsItemsEncoder: Encoder[Items[I]]
  ): F[Unit] =
    requestSession.post[Unit, Unit, Items[I]](
      Items(ids),
      uri"$baseUrl/delete",
      _ => ()
    )
}

class RawDatabases[F[_]](val requestSession: RequestSession[F])
    extends WithRequestSession[F]
    with Readable[RawDatabase, F]
    with Create[RawDatabase, RawDatabase, F]
    with DeleteByIds[F, String] {
  import RawDatabases._
  override val baseUrl = uri"${requestSession.baseUrl}/raw/dbs"

  override private[sdk] def readWithCursor(
      cursor: Option[String],
      limit: Option[Int],
      partition: Option[Partition]
  ): F[ItemsWithCursor[RawDatabase]] =
    Readable.readWithCursor(
      requestSession,
      baseUrl,
      cursor,
      limit,
      None,
      Constants.defaultBatchSize
    )

  override def createItems(items: Items[RawDatabase]): F[Seq[RawDatabase]] =
    Create.createItems[F, RawDatabase, RawDatabase](requestSession, baseUrl, items)

  override def deleteByIds(ids: Seq[String]): F[Unit] =
    RawResource.deleteByIds(requestSession, baseUrl, ids.map(RawDatabase.apply))
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
  override val baseUrl =
    uri"${requestSession.baseUrl}/raw/dbs/$database/tables"

  override private[sdk] def readWithCursor(
      cursor: Option[String],
      limit: Option[Int],
      partition: Option[Partition]
  ): F[ItemsWithCursor[RawTable]] =
    Readable.readWithCursor(
      requestSession,
      baseUrl,
      cursor,
      limit,
      None,
      Constants.defaultBatchSize
    )

  override def createItems(items: Items[RawTable]): F[Seq[RawTable]] =
    Create.createItems[F, RawTable, RawTable](requestSession, baseUrl, items)

  override def deleteByIds(ids: Seq[String]): F[Unit] =
    RawResource.deleteByIds(requestSession, baseUrl, ids.map(RawTable.apply))
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
  override val baseUrl =
    uri"${requestSession.baseUrl}/raw/dbs/$database/tables/$table/rows"

  val cursorsUri: Uri = uri"${requestSession.baseUrl}/raw/dbs/$database/tables/$table/cursors"

  // RAW does not return the created rows in the response, so we'll always return an empty sequence.
  override def createItems(items: Items[RawRow]): F[Seq[RawRow]] =
    requestSession.post[Seq[RawRow], Unit, Items[RawRow]](
      items,
      baseUrl,
      _ => Seq.empty[RawRow]
    )

  /** Creates RAW rows. When ensureParent=true it also creates the table and database if it does not
    * exist
    */
  def createItems(items: Items[RawRow], ensureParent: Boolean): F[Unit] =
    requestSession.post[Unit, Unit, Items[RawRow]](
      items,
      baseUrl.withParam("ensureParent", ensureParent.toString),
      identity
    )

  // ... and since RAW doesn't return the created rows, we just return the one we sent here.
  override def createOne(item: RawRow): F[RawRow] =
    requestSession.map(
      create(Seq(item)),
      (_: Seq[RawRow]) => item
    )

  def retrieveByKey(key: String): F[RawRow] =
    requestSession.get[RawRow, RawRow](
      uri"${requestSession.baseUrl}/raw/dbs/$database/tables/$table/rows/$key",
      value => value
    )

  override private[sdk] def readWithCursor(
      cursor: Option[String],
      limit: Option[Int],
      partition: Option[Partition]
  ): F[ItemsWithCursor[RawRow]] =
    Readable.readWithCursor(requestSession, baseUrl, cursor, limit, None, Constants.rowsBatchSize)

  override def deleteByIds(ids: Seq[String]): F[Unit] =
    RawResource.deleteByIds(requestSession, baseUrl, ids.map(RawRowKey.apply))

  override private[sdk] def filterWithCursor(
      filter: RawRowFilter,
      cursor: Option[String],
      limit: Option[Int],
      partition: Option[Partition],
      aggregatedProperties: Option[Seq[String]] = None
  ): F[ItemsWithCursor[RawRow]] =
    Readable.readWithCursor(
      requestSession,
      baseUrl.addParams(filterToParams(filter)),
      cursor,
      limit,
      None,
      Constants.rowsBatchSize
    )

  def getPartitionCursors(
      filter: RawRowFilter,
      numPartitions: Int
  ): F[Seq[String]] = {
    val cursorsUriWithParams = cursorsUri
      .addParam("numberOfCursors", numPartitions.toString)
      .addParams(lastUpdatedTimeFilterToParams(filter))
    requestSession
      .get[Seq[String], Items[String]](
        cursorsUriWithParams,
        value => value.items
      )
  }

  def filterOnePartition(
      filter: RawRowFilter,
      partitionCursor: String,
      limit: Option[Int] = None
  ): Stream[F, RawRow] =
    filterWithNextCursor(filter, Some(partitionCursor), limit, None)

  override def filterPartitionsF(
      filter: RawRowFilter,
      numPartitions: Int,
      limitPerPartition: Option[Int]
  )(implicit F: Applicative[F]): F[Seq[Stream[F, RawRow]]] =
    getPartitionCursors(filter, numPartitions).map { cursors =>
      cursors.map(filterOnePartition(filter, _, limitPerPartition))
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
    ).collect { case (key, Some(value)) =>
      key -> value
    } ++ lastUpdatedTimeFilterToParams(filter)

  def lastUpdatedTimeFilterToParams(filter: RawRowFilter): Map[String, String] =
    Map(
      "minLastUpdatedTime" -> filter.minLastUpdatedTime.map(_.toEpochMilli.toString),
      "maxLastUpdatedTime" -> filter.maxLastUpdatedTime.map(_.toEpochMilli.toString)
    ).collect { case (key, Some(value)) =>
      key -> value
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
