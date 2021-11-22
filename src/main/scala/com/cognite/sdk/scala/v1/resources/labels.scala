// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1.resources

import com.cognite.sdk.scala.common._
import com.cognite.sdk.scala.v1._
import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker
import sttp.client3._

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
      limit: Option[Int],
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
  implicit val labelCodec: JsonValueCodec[Label] = JsonCodecMaker.make
  implicit val labelsItemsWithCursorCodec: JsonValueCodec[ItemsWithCursor[Label]] =
    JsonCodecMaker.make
  implicit val labelItemsCodec: JsonValueCodec[Items[LabelCreate]] = JsonCodecMaker.make

  implicit val labelFilterCodec: JsonValueCodec[LabelsFilter] = JsonCodecMaker.make
  implicit val labelFilterRequestCodec: JsonValueCodec[FilterRequest[LabelsFilter]] =
    JsonCodecMaker.make
}
