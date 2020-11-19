// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1.resources

import com.cognite.sdk.scala.common._
import com.cognite.sdk.scala.v1._
import com.softwaremill.sttp._
import io.circe.derivation.deriveDecoder
import io.circe.Decoder

class Relationships[F[_]](val requestSession: RequestSession[F])
    extends WithRequestSession[F]
    with PartitionedReadable[Relationship, F]
    with RetrieveByExternalIdsWithIgnoreUnknownIds[Relationship, F] {
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
}

object Relationships {
  implicit val relationshipDecoder: Decoder[Relationship] = deriveDecoder[Relationship]
  implicit val relationshipItemsWithCursorDecoder: Decoder[ItemsWithCursor[Relationship]] =
    deriveDecoder[ItemsWithCursor[Relationship]]
  implicit val relationshipItemsDecoder: Decoder[Items[Relationship]] =
    deriveDecoder[Items[Relationship]]
  implicit val cogniteExternalIdDecoder: Decoder[CogniteExternalId] = deriveDecoder[CogniteExternalId]
}