// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1.resources

import com.cognite.sdk.scala.common._
import com.cognite.sdk.scala.v1._
import com.softwaremill.sttp._
import io.circe.{Decoder, Encoder}
import io.circe.derivation.{deriveDecoder, deriveEncoder}

class Labels[F[_]](val requestSession: RequestSession[F])
    extends WithRequestSession[F]
    with Create[Label, LabelCreate, F]
    with Filter[Label, LabelsFilter, F]
    with DeleteByExternalIds[F] {
  import Labels._
  override val baseUrl = uri"${requestSession.baseUrl}/labels"
  override def createItems(
      items: Items[LabelCreate]
  ): F[Seq[Label]] =
    Create.createItems[F, Label, LabelCreate](requestSession, baseUrl, items)

  override private[sdk] def filterWithCursor(
      filter: LabelsFilter,
      cursor: Option[String],
      limit: Option[StatusCode],
      partition: Option[Partition],
      aggregatedProperties: Option[Seq[String]]
  ): F[ItemsWithCursor[Label]] =
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

  override def deleteByExternalIds(externalIds: Seq[String]): F[Unit] =
    DeleteByExternalIds.deleteByExternalIds(requestSession, baseUrl, externalIds)
}

object Labels {
  implicit val labelDecoder: Decoder[Label] = deriveDecoder
  implicit val labelsItemsWithCursorDecoder: Decoder[ItemsWithCursor[Label]] =
    deriveDecoder

  implicit val labelEncoder: Encoder[LabelCreate] = deriveEncoder
  implicit val labelItemsEncoder: Encoder[Items[LabelCreate]] = deriveEncoder

  implicit val labelFilterEncoder: Encoder[LabelsFilter] = deriveEncoder
  implicit val labelFilterRequestEncoder: Encoder[FilterRequest[LabelsFilter]] =
    deriveEncoder
}
