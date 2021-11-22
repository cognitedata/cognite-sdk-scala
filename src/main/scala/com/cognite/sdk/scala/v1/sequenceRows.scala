// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1

import com.cognite.sdk.scala.common.ResponseWithCursor

final case class SequenceColumnSignature(
    externalId: String,
    name: Option[String],
    valueType: String = "DOUBLE"
)
final case class SequenceRow(rowNumber: Long, values: Seq[Json])
final case class SequenceRowsInsertById(id: Long, columns: Seq[String], rows: Seq[SequenceRow])
final case class SequenceRowsInsertByExternalId(
    externalId: String,
    columns: Seq[String],
    rows: Seq[SequenceRow]
)
final case class SequenceRowsDeleteById(id: Long, rows: Seq[Long])
final case class SequenceRowsDeleteByExternalId(externalId: String, rows: Seq[Long])
sealed trait SequenceRowsQuery {
  val start: Option[Long]
  val end: Option[Long]
  val limit: Option[Int]
  val cursor: Option[String]
  val columns: Option[Seq[String]]

  def withCursorAndLimit(newCursor: Option[String], limit: Option[Int]): SequenceRowsQuery
}
final case class SequenceRowsQueryById(
    id: Long,
    start: Option[Long],
    end: Option[Long],
    limit: Option[Int],
    cursor: Option[String],
    columns: Option[Seq[String]]
) extends SequenceRowsQuery {
  def withCursorAndLimit(newCursor: Option[String], limit: Option[Int]): SequenceRowsQuery =
    this.copy(cursor = newCursor, limit = limit)
}
final case class SequenceRowsQueryByExternalId(
    externalId: String,
    start: Option[Long],
    end: Option[Long],
    limit: Option[Int],
    cursor: Option[String],
    columns: Option[Seq[String]]
) extends SequenceRowsQuery {
  def withCursorAndLimit(newCursor: Option[String], limit: Option[Int]): SequenceRowsQuery =
    this.copy(cursor = newCursor, limit = limit)
}
final case class SequenceRowsResponse(
    id: Long,
    externalId: Option[String],
    columns: Seq[SequenceColumnSignature], // this can be empty if no data is returned
    rows: Seq[SequenceRow],
    nextCursor: Option[String]
) extends ResponseWithCursor
