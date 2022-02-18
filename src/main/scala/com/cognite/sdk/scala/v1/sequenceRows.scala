// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1

import com.cognite.sdk.scala.common.ResponseWithCursor
import io.circe.Json

final case class SequenceColumnSignature(
    externalId: String,
    name: Option[String],
    valueType: String = "DOUBLE"
)
final case class SequenceRow(rowNumber: Long, values: Seq[Json])
final case class SequenceRowsInsert(
    id: Option[Long],
    externalId: Option[String],
    columns: Seq[String],
    rows: Seq[SequenceRow]
) {
  require(id.isDefined || externalId.isDefined)
}
object SequenceRowsInsert {
  def apply(id: Long, columns: Seq[String], rows: Seq[SequenceRow]): SequenceRowsInsert =
    SequenceRowsInsert(Some(id), None, columns, rows)

  def apply(externalId: String, columns: Seq[String], rows: Seq[SequenceRow]): SequenceRowsInsert =
    SequenceRowsInsert(None, Some(externalId), columns, rows)

  def apply(
      cogniteId: CogniteId,
      columns: Seq[String],
      rows: Seq[SequenceRow]
  ): SequenceRowsInsert =
    cogniteId match {
      case id: CogniteInternalId =>
        SequenceRowsInsert(id.id, columns, rows)
      case id: CogniteExternalId =>
        SequenceRowsInsert(id.externalId, columns, rows)
    }
}

final case class SequenceRowsDelete(id: Option[Long], externalId: Option[String], rows: Seq[Long]) {
  require(id.isDefined || externalId.isDefined)
}
object SequenceRowsDelete {
  def apply(id: Long, rows: Seq[Long]): SequenceRowsDelete =
    SequenceRowsDelete(Some(id), None, rows)

  def apply(externalId: String, rows: Seq[Long]): SequenceRowsDelete =
    SequenceRowsDelete(None, Some(externalId), rows)

  def apply(
      cogniteId: CogniteId,
      rows: Seq[Long]
  ): SequenceRowsDelete =
    cogniteId match {
      case id: CogniteInternalId =>
        SequenceRowsDelete(id.id, rows)
      case id: CogniteExternalId =>
        SequenceRowsDelete(id.externalId, rows)
    }
}

final case class SequenceRowsQuery(
    id: Option[Long],
    externalId: Option[String],
    start: Option[Long],
    end: Option[Long],
    limit: Option[Int],
    cursor: Option[String],
    columns: Option[Seq[String]]
) {
  require(id.isDefined || externalId.isDefined)
  def withCursorAndLimit(
      newCursor: Option[String],
      limit: Option[Int]
  ): SequenceRowsQuery =
    this.copy(cursor = newCursor, limit = limit)
}

object SequenceRowsQuery {
  def apply(
      id: Long,
      start: Option[Long],
      end: Option[Long],
      limit: Option[Int],
      cursor: Option[String],
      columns: Option[Seq[String]]
  ): SequenceRowsQuery =
    SequenceRowsQuery(Some(id), None, start, end, limit, cursor, columns)

  def apply(
      externalId: String,
      start: Option[Long],
      end: Option[Long],
      limit: Option[Int],
      cursor: Option[String],
      columns: Option[Seq[String]]
  ): SequenceRowsQuery =
    SequenceRowsQuery(None, Some(externalId), start, end, limit, cursor, columns)

  def apply(
      cogniteId: CogniteId,
      start: Option[Long],
      end: Option[Long],
      limit: Option[Int],
      cursor: Option[String],
      columns: Option[Seq[String]]
  ): SequenceRowsQuery =
    cogniteId match {
      case id: CogniteInternalId =>
        SequenceRowsQuery(id.id, start, end, limit, cursor, columns)
      case id: CogniteExternalId =>
        SequenceRowsQuery(id.externalId, start, end, limit, cursor, columns)
    }
}

final case class SequenceRowsResponse(
    id: Long,
    externalId: Option[String],
    columns: Seq[SequenceColumnSignature], // this can be empty if no data is returned
    rows: Seq[SequenceRow],
    nextCursor: Option[String]
) extends ResponseWithCursor
