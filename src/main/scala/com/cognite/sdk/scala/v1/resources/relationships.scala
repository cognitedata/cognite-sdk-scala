// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1.resources

import com.cognite.sdk.scala.common._
import com.cognite.sdk.scala.v1._
import sttp.client3._
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

class Relationships[F[_]](val requestSession: RequestSession[F])
    extends WithRequestSession[F]
    with PartitionedReadable[Relationship, F]
    with Filter[Relationship, RelationshipsFilter, F]
    with RetrieveByIdsWithIgnoreUnknownIds[Relationship, F]
    with RetrieveByExternalIdsWithIgnoreUnknownIds[Relationship, F]
    with DeleteByExternalIdsWithIgnoreUnknownIds[F]
    with Create[Relationship, RelationshipCreate, F]
    with UpdateByExternalId[Relationship, RelationshipUpdate, F] {
  import Relationships._
  override val baseUrl = uri"${requestSession.baseUrl}/relationships"

  override private[sdk] def readWithCursor(
      cursor: Option[String],
      limit: Option[Int],
      partition: Option[Partition]
  ): F[ItemsWithCursor[Relationship]] =
    Readable.readWithCursor(
      requestSession,
      baseUrl,
      cursor,
      limit,
      partition,
      Constants.defaultBatchSize
    )

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
    DeleteByCogniteIds.deleteWithIgnoreUnknownIds(
      requestSession,
      baseUrl,
      externalIds.map(CogniteExternalId.apply),
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
      uri"$baseUrl/list",
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
  implicit val relationshipDecoder: Decoder[Relationship] = deriveDecoder[Relationship]
  implicit val relationshipItemsWithCursorDecoder: Decoder[ItemsWithCursor[Relationship]] =
    deriveDecoder[ItemsWithCursor[Relationship]]
  implicit val relationshipItemsDecoder: Decoder[Items[Relationship]] =
    deriveDecoder[Items[Relationship]]
  implicit val cogniteExternalIdDecoder: Decoder[CogniteExternalId] =
    deriveDecoder[CogniteExternalId]
  implicit val createRelationEncoder: Encoder[RelationshipCreate] =
    deriveEncoder[RelationshipCreate]
  implicit val createRelationsItemsEncoder: Encoder[Items[RelationshipCreate]] =
    deriveEncoder[Items[RelationshipCreate]]
  implicit val relationshipsFilterEncoder: Encoder[RelationshipsFilter] =
    deriveEncoder[RelationshipsFilter]
  implicit val relationshipsFilterRequestEncoder: Encoder[FilterRequest[RelationshipsFilter]] =
    deriveEncoder[FilterRequest[RelationshipsFilter]]
  implicit val confidenceRangeEncoder: Encoder[ConfidenceRange] = deriveEncoder[ConfidenceRange]
  implicit val relationshipUpdateEncoder: Encoder[RelationshipUpdate] =
    deriveEncoder[RelationshipUpdate]
}
