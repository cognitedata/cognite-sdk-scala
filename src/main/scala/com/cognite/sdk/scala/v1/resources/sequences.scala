// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1.resources
import com.cognite.sdk.scala.common._
import com.cognite.sdk.scala.v1._
import sttp.client3._
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

class SequencesResource[F[_]](val requestSession: RequestSession[F])
    extends WithRequestSession[F]
    with PartitionedReadable[Sequence, F]
    with RetrieveByIdsWithIgnoreUnknownIds[Sequence, F]
    with RetrieveByExternalIdsWithIgnoreUnknownIds[Sequence, F]
    with Create[Sequence, SequenceCreate, F]
    with DeleteByIds[F, Long]
    with DeleteByExternalIds[F]
    with PartitionedFilter[Sequence, SequenceFilter, F]
    with Search[Sequence, SequenceQuery, F]
    with UpdateById[Sequence, SequenceUpdate, F]
    with UpdateByExternalId[Sequence, SequenceUpdate, F] {
  import SequencesResource._

  override val baseUrl = uri"${requestSession.baseUrl}/sequences"

  override private[sdk] def readWithCursor(
      cursor: Option[String],
      limit: Option[Int],
      partition: Option[Partition]
  ): F[ItemsWithCursor[Sequence]] =
    Readable.readWithCursor(
      requestSession,
      baseUrl,
      cursor,
      limit,
      partition,
      Constants.defaultBatchSize
    )

  override def retrieveByIds(
      ids: Seq[Long],
      ignoreUnknownIds: Boolean
  ): F[Seq[Sequence]] =
    RetrieveByIdsWithIgnoreUnknownIds.retrieveByIds(
      requestSession,
      baseUrl,
      ids,
      ignoreUnknownIds
    )

  override def retrieveByExternalIds(
      externalIds: Seq[String],
      ignoreUnknownIds: Boolean
  ): F[Seq[Sequence]] =
    RetrieveByExternalIdsWithIgnoreUnknownIds.retrieveByExternalIds(
      requestSession,
      baseUrl,
      externalIds,
      ignoreUnknownIds
    )

  override def createItems(items: Items[SequenceCreate]): F[Seq[Sequence]] =
    Create.createItems[F, Sequence, SequenceCreate](requestSession, baseUrl, items)

  override def updateById(items: Map[Long, SequenceUpdate]): F[Seq[Sequence]] =
    UpdateById.updateById[F, Sequence, SequenceUpdate](requestSession, baseUrl, items)

  override def updateByExternalId(items: Map[String, SequenceUpdate]): F[Seq[Sequence]] =
    UpdateByExternalId.updateByExternalId[F, Sequence, SequenceUpdate](
      requestSession,
      baseUrl,
      items
    )

  override def deleteByIds(ids: Seq[Long]): F[Unit] =
    DeleteByIds.deleteByIds(requestSession, baseUrl, ids)

  override def deleteByExternalIds(externalIds: Seq[String]): F[Unit] =
    DeleteByExternalIds.deleteByExternalIds(requestSession, baseUrl, externalIds)

  override private[sdk] def filterWithCursor(
      filter: SequenceFilter,
      cursor: Option[String],
      limit: Option[Int],
      partition: Option[Partition],
      aggregatedProperties: Option[Seq[String]]
  ) =
    Filter.filterWithCursor(
      requestSession,
      uri"$baseUrl/list",
      filter,
      cursor,
      limit,
      partition,
      Constants.defaultBatchSize
    )

  override def search(searchQuery: SequenceQuery): F[Seq[Sequence]] =
    Search.search(requestSession, baseUrl, searchQuery)
}

object SequencesResource {
  implicit val sequenceColumnEncoder: Encoder[SequenceColumn] = deriveEncoder
  implicit val sequenceColumnCreateEncoder: Encoder[SequenceColumnCreate] = deriveEncoder
  @SuppressWarnings(Array("org.wartremover.warts.JavaSerializable"))
  implicit val sequenceColumnDecoder: Decoder[SequenceColumn] = deriveDecoder
  implicit val sequenceDecoder: Decoder[Sequence] = deriveDecoder[Sequence]
  implicit val sequenceColumnModifyEncoder: Encoder[SequenceColumnModify] =
    deriveEncoder[SequenceColumnModify]
  implicit val sequenceColumnModifyUpdateEncoder: Encoder[SequenceColumnModifyUpdate] =
    deriveEncoder[SequenceColumnModifyUpdate]
  implicit val sequenceColumnsUpdateEncoder: Encoder[SequenceColumnsUpdate] =
    deriveEncoder[SequenceColumnsUpdate]
  implicit val sequenceUpdateEncoder: Encoder[SequenceUpdate] = deriveEncoder[SequenceUpdate]
  implicit val sequenceItemsWithCursorDecoder: Decoder[ItemsWithCursor[Sequence]] =
    deriveDecoder[ItemsWithCursor[Sequence]]
  implicit val sequenceItemsDecoder: Decoder[Items[Sequence]] =
    deriveDecoder[Items[Sequence]]
  implicit val createSequenceEncoder: Encoder[SequenceCreate] = deriveEncoder[SequenceCreate]
  implicit val createSequenceItemsEncoder: Encoder[Items[SequenceCreate]] =
    deriveEncoder[Items[SequenceCreate]]
  implicit val sequenceFilterEncoder: Encoder[SequenceFilter] =
    deriveEncoder[SequenceFilter]
  implicit val sequenceSearchEncoder: Encoder[SequenceSearch] =
    deriveEncoder[SequenceSearch]
  implicit val sequenceQueryEncoder: Encoder[SequenceQuery] =
    deriveEncoder[SequenceQuery]
  implicit val sequenceFilterRequestEncoder: Encoder[FilterRequest[SequenceFilter]] =
    deriveEncoder[FilterRequest[SequenceFilter]]
}
