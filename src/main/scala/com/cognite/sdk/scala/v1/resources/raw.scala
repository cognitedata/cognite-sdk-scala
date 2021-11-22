// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1.resources

import com.cognite.sdk.scala.common._
import com.cognite.sdk.scala.v1._
import sttp.client3._
import sttp.client3.jsoniter_scala._
import com.github.plokhotnyuk.jsoniter_scala.macros._
import com.github.plokhotnyuk.jsoniter_scala.core._
import sttp.model.Uri

object RawResource {
  def deleteByIds[F[_], I](requestSession: RequestSession[F], baseUrl: Uri, ids: Seq[I])(
      implicit idsItemsCodec: JsonValueCodec[Items[I]]
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
  implicit val rawDatabaseItemsWithCursorCodec: JsonValueCodec[ItemsWithCursor[RawDatabase]] =
    JsonCodecMaker.make[ItemsWithCursor[RawDatabase]]
  implicit val rawDatabaseItemsCodec: JsonValueCodec[Items[RawDatabase]] =
    JsonCodecMaker.make[Items[RawDatabase]]
  implicit val rawDatabaseCodec: JsonValueCodec[RawDatabase] = JsonCodecMaker.make[RawDatabase]
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
  implicit val rawTableItemsWithCursorCodec: JsonValueCodec[ItemsWithCursor[RawTable]] =
    JsonCodecMaker.make[ItemsWithCursor[RawTable]]
  implicit val rawTableItemsCodec: JsonValueCodec[Items[RawTable]] = JsonCodecMaker.make[Items[RawTable]]
  implicit val rawTableCodec: JsonValueCodec[RawTable] = JsonCodecMaker.make[RawTable]
}

class RawRows[F[_]](val requestSession: RequestSession[F], database: String, table: String)
    extends WithRequestSession[F]
    with Readable[RawRow, F]
    with Create[RawRow, RawRow, F]
    with DeleteByIds[F, String] {
  implicit val stringItemsCodec: JsonValueCodec[Items[String]] = JsonCodecMaker.make[Items[String]]

  implicit val errorOrStringItemsCodec: JsonValueCodec[Either[CdpApiError, Items[String]]] =
    JsonCodecMaker.make

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

  override private[sdk] def readWithCursor(
      cursor: Option[String],
      limit: Option[Int],
      partition: Option[Partition]
  ): F[ItemsWithCursor[RawRow]] =
    Readable.readWithCursor(requestSession, baseUrl, cursor, limit, None, Constants.rowsBatchSize)

  override def deleteByIds(ids: Seq[String]): F[Unit] =
    RawResource.deleteByIds(requestSession, baseUrl, ids.map(RawRowKey.apply))

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
  implicit val rawRowCodec: JsonValueCodec[RawRow] = JsonCodecMaker.make[RawRow]
  implicit val rawRowItemsWithCursorCodec: JsonValueCodec[ItemsWithCursor[RawRow]] =
    JsonCodecMaker.make[ItemsWithCursor[RawRow]]
  implicit val rawRowItemsCodec: JsonValueCodec[Items[RawRow]] =
    JsonCodecMaker.make[Items[RawRow]]

  implicit val rawRowKeyCodec: JsonValueCodec[RawRowKey] = JsonCodecMaker.make[RawRowKey]
  implicit val rawRowKeyItemsCodec: JsonValueCodec[Items[RawRowKey]] =
    JsonCodecMaker.make[Items[RawRowKey]]
}
