package com.cognite.sdk.scala.v1

import io.circe.Json

final case class SequenceColumnId(externalId: String)
final case class SequenceRow(rowNumber: Long, values: Seq[Json])
final case class SequenceRowsInsertById(id: Long, columns: Seq[String], rows: Seq[SequenceRow])
final case class SequenceRowsInsertByExternalId(
    externalId: String,
    columns: Seq[String],
    rows: Seq[SequenceRow]
)
final case class SequenceRowsDeleteById(id: Long, rows: Seq[Long])
final case class SequenceRowsDeleteByExternalId(externalId: String, rows: Seq[Long])
final case class SequenceRowsQueryById(
    id: Long,
    start: Long,
    end: Long,
    limit: Option[Int],
    columns: Option[Seq[String]]
)
final case class SequenceRowsQueryByExternalId(
    externalId: String,
    start: Long,
    end: Long,
    limit: Option[Int],
    columns: Option[Seq[String]]
)
final case class SequenceRowsResponse(
    columns: Seq[SequenceColumnId], // this can be empty if no data is returned
    rows: Seq[SequenceRow]
)
