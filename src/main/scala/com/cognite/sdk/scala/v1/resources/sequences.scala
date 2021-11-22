// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1.resources
import com.cognite.sdk.scala.common._
import com.cognite.sdk.scala.v1._
import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker
import sttp.client3._

class SequencesResource[F[_]](val requestSession: RequestSession[F])
    extends WithRequestSession[F]
    with RetrieveByIdsWithIgnoreUnknownIds[Sequence, F]
    with RetrieveByExternalIdsWithIgnoreUnknownIds[Sequence, F]
    with Create[Sequence, SequenceCreate, F]
    with DeleteByIds[F, Long]
    with DeleteByExternalIds[F]
    with Search[Sequence, SequenceQuery, F]
    with UpdateById[Sequence, SequenceUpdate, F]
    with UpdateByExternalId[Sequence, SequenceUpdate, F] {
  import SequencesResource._

  override val baseUrl = uri"${requestSession.baseUrl}/sequences"

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

  override def search(searchQuery: SequenceQuery): F[Seq[Sequence]] =
    Search.search(requestSession, baseUrl, searchQuery)
}

object SequencesResource {
  implicit val sequenceColumnCodec: JsonValueCodec[SequenceColumn] = JsonCodecMaker.make[SequenceColumn]
  implicit val sequenceColumnCreateCodec: JsonValueCodec[SequenceColumnCreate] = JsonCodecMaker.make[SequenceColumnCreate]
  implicit val sequenceCodec: JsonValueCodec[Sequence] = JsonCodecMaker.make[Sequence]
  implicit val sequenceUpdateCodec: JsonValueCodec[SequenceUpdate] = JsonCodecMaker.make[SequenceUpdate]
  implicit val sequenceItemsWithCursorCodec: JsonValueCodec[ItemsWithCursor[Sequence]] =
    JsonCodecMaker.make[ItemsWithCursor[Sequence]]
  implicit val sequenceItemsCodec: JsonValueCodec[Items[Sequence]] =
    JsonCodecMaker.make[Items[Sequence]]
  implicit val createSequenceCodec: JsonValueCodec[SequenceCreate] = JsonCodecMaker.make[SequenceCreate]
  implicit val createSequenceItemsCodec: JsonValueCodec[Items[SequenceCreate]] =
    JsonCodecMaker.make[Items[SequenceCreate]]
  implicit val sequenceFilterCodec: JsonValueCodec[SequenceFilter] =
    JsonCodecMaker.make[SequenceFilter]
  implicit val sequenceSearchCodec: JsonValueCodec[SequenceSearch] =
    JsonCodecMaker.make[SequenceSearch]
  implicit val sequenceQueryCodec: JsonValueCodec[SequenceQuery] =
    JsonCodecMaker.make[SequenceQuery]
}
