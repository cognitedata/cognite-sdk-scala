// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1.resources

import com.cognite.sdk.scala.common._
import com.cognite.sdk.scala.v1._
import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker
import sttp.client3._

class Relationships[F[_]](val requestSession: RequestSession[F])
    extends WithRequestSession[F]
    with Filter[Relationship, RelationshipsFilter, F]
    with RetrieveByIdsWithIgnoreUnknownIds[Relationship, F]
    with RetrieveByExternalIdsWithIgnoreUnknownIds[Relationship, F]
    with DeleteByExternalIdsWithIgnoreUnknownIds[F]
    with Create[Relationship, RelationshipCreate, F]
    with UpdateByExternalId[Relationship, RelationshipUpdate, F] {
  import Relationships._
  override val baseUrl = uri"${requestSession.baseUrl}/relationships"

  override def retrieveByExternalIds(
      externalIds: Seq[String],
      ignoreUnknownIds: Boolean
  ): F[Seq[Relationship]] =
    RetrieveByExternalIdsWithIgnoreUnknownIds.retrieveByExternalIds(
      requestSession,
      baseUrl,
      externalIds,
      ignoreUnknownIds
    )

  override def createItems(items: Items[RelationshipCreate]): F[Seq[Relationship]] =
    Create.createItems[F, Relationship, RelationshipCreate](requestSession, baseUrl, items)

  override def deleteByExternalIds(externalIds: Seq[String]): F[Unit] =
    deleteByExternalIds(externalIds, false)

  override def deleteByExternalIds(
      externalIds: Seq[String],
      ignoreUnknownIds: Boolean = false
  ): F[Unit] =
    DeleteByExternalIds.deleteByExternalIdsWithIgnoreUnknownIds(
      requestSession,
      baseUrl,
      externalIds,
      ignoreUnknownIds
    )

  override private[sdk] def filterWithCursor(
      filter: RelationshipsFilter,
      cursor: Option[String],
      limit: Option[Int],
      partition: Option[Partition],
      aggregatedProperties: Option[Seq[String]]
  ): F[ItemsWithCursor[Relationship]] =
    Filter.filterWithCursor(
      requestSession,
      baseUrl,
      filter,
      cursor,
      limit,
      partition,
      Constants.defaultBatchSize,
      aggregatedProperties
    )

  override def updateByExternalId(items: Map[String, RelationshipUpdate]): F[Seq[Relationship]] =
    UpdateByExternalId.updateByExternalId[F, Relationship, RelationshipUpdate](
      requestSession,
      baseUrl,
      items
    )

  override def retrieveByIds(ids: Seq[Long], ignoreUnknownIds: Boolean): F[Seq[Relationship]] =
    RetrieveByIdsWithIgnoreUnknownIds.retrieveByIds(
      requestSession,
      baseUrl,
      ids,
      ignoreUnknownIds
    )
}

object Relationships {
  implicit val relationshipCodec: JsonValueCodec[Relationship] = JsonCodecMaker.make[Relationship]
  implicit val relationshipItemsWithCursorCodec: JsonValueCodec[ItemsWithCursor[Relationship]] =
    JsonCodecMaker.make[ItemsWithCursor[Relationship]]
  implicit val relationshipItemsCodec: JsonValueCodec[Items[Relationship]] =
    JsonCodecMaker.make[Items[Relationship]]
  implicit val cogniteExternalIdCodec: JsonValueCodec[CogniteExternalId] =
    JsonCodecMaker.make[CogniteExternalId]
  implicit val createRelationCodec: JsonValueCodec[RelationshipCreate] =
    JsonCodecMaker.make[RelationshipCreate]
  implicit val createRelationsItemsCodec: JsonValueCodec[Items[RelationshipCreate]] =
    JsonCodecMaker.make[Items[RelationshipCreate]]
  implicit val relationshipsFilterCodec: JsonValueCodec[RelationshipsFilter] =
    JsonCodecMaker.make[RelationshipsFilter]
  implicit val relationshipsFilterRequestCodec: JsonValueCodec[FilterRequest[RelationshipsFilter]] =
    JsonCodecMaker.make[FilterRequest[RelationshipsFilter]]
  implicit val confidenceRangeCodec: JsonValueCodec[ConfidenceRange] = JsonCodecMaker.make[ConfidenceRange]
  implicit val relationshipUpdateCodec: JsonValueCodec[RelationshipUpdate] =
    JsonCodecMaker.make[RelationshipUpdate]
}
