package com.cognite.sdk.scala.v1

import cats.data.NonEmptyList
import com.cognite.sdk.scala.common.CogniteId
import io.circe.Json

final case class SequenceColumnId(id: Long, externalId: Option[String])
final case class SequenceRow(rowNumber: Long, values: Seq[Json])
final case class SequenceRowsInsertById(id: Long, columns: Seq[CogniteId], rows: Seq[SequenceRow])
final case class SequenceRowsInsertByExternalId(
    externalId: String,
    columns: Seq[CogniteExternalId],
    rows: Seq[SequenceRow]
)
final case class SequenceRowsDeleteById(id: Long, rows: Seq[Long])
final case class SequenceRowsDeleteByExternalId(externalId: String, rows: Seq[Long])
final case class SequenceRowsQueryById(
    id: Long,
    inclusiveFrom: Long,
    exclusiveTo: Long,
    limit: Int,
    columns: Option[Seq[Long]]
)
final case class SequenceRowsQueryByExternalId(
    externalId: String,
    inclusiveFrom: Long,
    exclusiveTo: Long,
    limit: Int,
    columns: Option[Seq[String]]
)
final case class SequenceRowsResponse(
    columns: NonEmptyList[SequenceColumnId],
    rows: Seq[SequenceRow]
)
