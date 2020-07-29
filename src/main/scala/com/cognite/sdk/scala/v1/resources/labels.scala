package com.cognite.sdk.scala.v1.resources

import com.cognite.sdk.scala.common.{DeleteByExternalIds, _}
import com.cognite.sdk.scala.v1._
import com.softwaremill.sttp._
import io.circe.{Decoder, Encoder}
import io.circe.derivation.{deriveDecoder, deriveEncoder}

class Labels[F[_]](val requestSession: RequestSession[F])
    extends WithRequestSession[F]
    with Create[Label, LabelCreate, F]
    with Filter[Label, LabelFilter, F]
    with DeleteByExternalIds[F] {
  import Labels._

  override val baseUrl = uri"${requestSession.baseUrl}/labels"

  override def createItems(items: Items[LabelCreate]): F[Seq[Label]] =
    Create.createItems[F, Label, LabelCreate](requestSession, baseUrl, items)

  override private[sdk] def filterWithCursor(
      filter: LabelFilter,
      cursor: Option[String],
      limit: Option[Int],
      partition: Option[Partition],
      aggregatedProperties: Option[Seq[String]]
  ): F[ItemsWithCursor[Label]] = Filter.filterWithCursor(
    requestSession,
    baseUrl,
    filter,
    cursor,
    limit,
    partition = None,
    Constants.defaultBatchSize
  )

  override def deleteByExternalIds(externalIds: Seq[String]): F[Unit] =
    DeleteByExternalIds.deleteByExternalIds(requestSession, baseUrl, externalIds)

}

object Labels {
  implicit val labelDecoder: Decoder[Label] = deriveDecoder
  implicit val labelItemsWithCursorDecoder: Decoder[ItemsWithCursor[Label]] = deriveDecoder
  implicit val labelItemsDecoder: Decoder[Items[Label]] = deriveDecoder
  implicit val labelCreateEncoder: Encoder[LabelCreate] = deriveEncoder
  implicit val labelCreateItemsEncoder: Encoder[Items[LabelCreate]] = deriveEncoder
  implicit val labelFilterEncoder: Encoder[LabelFilter] = deriveEncoder
  implicit val labelFilterRequestEncoder: Encoder[FilterRequest[LabelFilter]] = deriveEncoder
  implicit val labelListQueryEncoder: Encoder[LabelQuery] = deriveEncoder
}
