// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1.resources

import cats.Monad
import cats.syntax.all._
import com.cognite.sdk.scala.common._
import com.cognite.sdk.scala.v1._
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import sttp.client3._
import sttp.client3.circe._

class Transformations[F[_]: Monad](val requestSession: RequestSession[F])
    extends WithRequestSession[F]
    with Create[TransformationRead, TransformationCreate, F]
    with RetrieveByIdsWithIgnoreUnknownIds[TransformationRead, F]
    with Readable[TransformationRead, F]
    with RetrieveByExternalIdsWithIgnoreUnknownIds[TransformationRead, F]
    with DeleteByIdsWithIgnoreUnknownIds[F, Long]
    with DeleteByExternalIdsWithIgnoreUnknownIds[F]
    with UpdateById[TransformationRead, TransformationUpdate, F]
    with UpdateByExternalId[TransformationRead, TransformationUpdate, F] {
  import Transformations._
  override val baseUrl = uri"${requestSession.baseUrl}/transformations"

  val schedules: TransformationSchedules[F] = new TransformationSchedules[F](requestSession)

  override private[sdk] def readWithCursor(
      cursor: Option[String],
      limit: Option[Int],
      partition: Option[Partition]
  ): F[ItemsWithCursor[TransformationRead]] =
    Readable.readWithCursor(
      requestSession,
      baseUrl,
      cursor,
      limit,
      partition,
      1000
    )

  override def retrieveByIds(
      ids: Seq[Long],
      ignoreUnknownIds: Boolean
  ): F[Seq[TransformationRead]] =
    RetrieveByIdsWithIgnoreUnknownIds.retrieveByIds(
      requestSession,
      baseUrl,
      ids,
      ignoreUnknownIds
    )

  override def retrieveByExternalIds(
      externalIds: Seq[String],
      ignoreUnknownIds: Boolean
  ): F[Seq[TransformationRead]] =
    RetrieveByExternalIdsWithIgnoreUnknownIds.retrieveByExternalIds(
      requestSession,
      baseUrl,
      externalIds,
      ignoreUnknownIds
    )

  override def createItems(items: Items[TransformationCreate]): F[Seq[TransformationRead]] =
    Create.createItems[F, TransformationRead, TransformationCreate](
      requestSession,
      baseUrl,
      items
    )

  override def updateById(
      items: Map[Long, TransformationUpdate]
  ): F[Seq[TransformationRead]] =
    UpdateById.updateById[F, TransformationRead, TransformationUpdate](
      requestSession,
      baseUrl,
      items
    )

  override def updateByExternalId(
      items: Map[String, TransformationUpdate]
  ): F[Seq[TransformationRead]] =
    UpdateByExternalId.updateByExternalId[F, TransformationRead, TransformationUpdate](
      requestSession,
      baseUrl,
      items
    )

  override def deleteByIds(ids: Seq[Long]): F[Unit] = deleteByIds(ids, false)

  override def deleteByIds(ids: Seq[Long], ignoreUnknownIds: Boolean): F[Unit] =
    DeleteByIds.deleteByIdsWithIgnoreUnknownIds(requestSession, baseUrl, ids, ignoreUnknownIds)

  override def deleteByExternalIds(externalIds: Seq[String]): F[Unit] =
    deleteByExternalIds(externalIds, false)

  override def deleteByExternalIds(externalIds: Seq[String], ignoreUnknownIds: Boolean): F[Unit] =
    DeleteByExternalIds.deleteByExternalIdsWithIgnoreUnknownIds(
      requestSession,
      baseUrl,
      externalIds,
      ignoreUnknownIds
    )

  def query[I](
      query: String,
      sourceLimit: Option[Int],
      limit: Int = 1000
  )(implicit itemDecoder: Decoder[I]): F[QueryResponse[I]] = {
    implicit val responseItemsDecoder: Decoder[Items[I]] = deriveDecoder[Items[I]]
    val _ = itemDecoder.hashCode + responseItemsDecoder.hashCode // suppress no usage warnings... 🤦
    implicit val responseDecoder: Decoder[QueryResponse[I]] = deriveDecoder[QueryResponse[I]]
    requestSession.post[QueryResponse[I], QueryResponse[I], QueryQuery](
      QueryQuery(query),
      uri"$baseUrl/query/run"
        .addParam("limit", limit.toString)
        .addParam("sourceLimit", sourceLimit.map(_.toString).getOrElse("all")),
      identity
    )
  }

  @SuppressWarnings(Array("org.wartremover.warts.TraversableOps"))
  def queryOne[I](
      q: String,
      sourceLimit: Option[Int] = None
  )(implicit itemDecoder: Decoder[I]): F[I] =
    query[I](q, sourceLimit, limit = 1).map(_.results.items.head)
}

object Transformations {
  implicit val scheduleReadDecoder: Decoder[TransformationScheduleRead] = deriveDecoder[TransformationScheduleRead]
  implicit val readDecoder: Decoder[TransformationRead] = deriveDecoder[TransformationRead]
  implicit val readItemsWithCursorDecoder: Decoder[ItemsWithCursor[TransformationRead]] =
    deriveDecoder[ItemsWithCursor[TransformationRead]]
  implicit val readItemsDecoder: Decoder[Items[TransformationRead]] =
    deriveDecoder[Items[TransformationRead]]
  implicit val createEncoder: Encoder[TransformationCreate] = deriveEncoder[TransformationCreate]
  implicit val createItemsEncoder: Encoder[Items[TransformationCreate]] =
    deriveEncoder[Items[TransformationCreate]]
  implicit val updateEncoder: Encoder[TransformationUpdate] =
    deriveEncoder[TransformationUpdate]

  implicit val errorOrUnitDecoder: Decoder[Either[CdpApiError, Unit]] =
    EitherDecoder.eitherDecoder[CdpApiError, Unit]
  implicit val deleteRequestWithRecursiveAndIgnoreUnknownIdsEncoder
      : Encoder[ItemsWithRecursiveAndIgnoreUnknownIds] =
    deriveEncoder[ItemsWithRecursiveAndIgnoreUnknownIds]
  implicit val cogniteExternalIdDecoder: Decoder[CogniteExternalId] =
    deriveDecoder[CogniteExternalId]
  implicit val queryEncoder: Encoder[QueryQuery] =
    deriveEncoder[QueryQuery]
  implicit val querySchemaDecoder: Decoder[QuerySchemaColumn] = deriveDecoder[QuerySchemaColumn]
  implicit val querySchemaItemsDecoder: Decoder[Items[QuerySchemaColumn]] =
    deriveDecoder[Items[QuerySchemaColumn]]
}

class TransformationSchedules[F[_]](val requestSession: RequestSession[F])
    extends WithRequestSession[F]
    with Create[TransformationScheduleRead, TransformationScheduleCreate, F]
    with RetrieveByIdsWithIgnoreUnknownIds[TransformationScheduleRead, F]
    with Readable[TransformationScheduleRead, F]
    with RetrieveByExternalIdsWithIgnoreUnknownIds[TransformationScheduleRead, F]
    with DeleteByIdsWithIgnoreUnknownIds[F, Long]
    with DeleteByExternalIdsWithIgnoreUnknownIds[F]
    //with UpdateById[TransformConfigRead, StandardTransformConfigUpdate, F]
    //with UpdateByExternalId[TransformConfigRead, StandardTransformConfigUpdate, F]
    {
  import TransformationSchedules._
  override val baseUrl = uri"${requestSession.baseUrl}/transformations/schedules"

  override private[sdk] def readWithCursor(
      cursor: Option[String],
      limit: Option[Int],
      partition: Option[Partition]
  ): F[ItemsWithCursor[TransformationScheduleRead]] =
    Readable.readWithCursor(
      requestSession,
      baseUrl,
      cursor,
      limit,
      partition,
      100
    )

  override def retrieveByIds(
      ids: Seq[Long],
      ignoreUnknownIds: Boolean
  ): F[Seq[TransformationScheduleRead]] =
    RetrieveByIdsWithIgnoreUnknownIds.retrieveByIds(
      requestSession,
      baseUrl,
      ids,
      ignoreUnknownIds
    )

  override def retrieveByExternalIds(
      externalIds: Seq[String],
      ignoreUnknownIds: Boolean
  ): F[Seq[TransformationScheduleRead]] =
    RetrieveByExternalIdsWithIgnoreUnknownIds.retrieveByExternalIds(
      requestSession,
      baseUrl,
      externalIds,
      ignoreUnknownIds
    )

  override def createItems(
      items: Items[TransformationScheduleCreate]
  ): F[Seq[TransformationScheduleRead]] =
    Create.createItems[F, TransformationScheduleRead, TransformationScheduleCreate](
      requestSession,
      baseUrl,
      items
    )

  override def deleteByIds(ids: Seq[Long]): F[Unit] = deleteByIds(ids, false)

  override def deleteByIds(ids: Seq[Long], ignoreUnknownIds: Boolean): F[Unit] =
    DeleteByIds.deleteByIdsWithIgnoreUnknownIds(requestSession, baseUrl, ids, ignoreUnknownIds)

  override def deleteByExternalIds(externalIds: Seq[String]): F[Unit] =
    deleteByExternalIds(externalIds, false)

  override def deleteByExternalIds(externalIds: Seq[String], ignoreUnknownIds: Boolean): F[Unit] =
    DeleteByExternalIds.deleteByExternalIdsWithIgnoreUnknownIds(
      requestSession,
      baseUrl,
      externalIds,
      ignoreUnknownIds
    )
}

object TransformationSchedules {
  implicit val readDecoder: Decoder[TransformationScheduleRead] =
    deriveDecoder[TransformationScheduleRead]
  implicit val readItemsWithCursorDecoder: Decoder[ItemsWithCursor[TransformationScheduleRead]] =
    deriveDecoder[ItemsWithCursor[TransformationScheduleRead]]
  implicit val readItemsDecoder: Decoder[Items[TransformationScheduleRead]] =
    deriveDecoder[Items[TransformationScheduleRead]]
  implicit val createEncoder: Encoder[TransformationScheduleCreate] =
    deriveEncoder[TransformationScheduleCreate]
  implicit val createItemsEncoder: Encoder[Items[TransformationScheduleCreate]] =
    deriveEncoder[Items[TransformationScheduleCreate]]
  //implicit val updateEncoder: Encoder[StandardTransformConfigUpdate] = deriveEncoder[StandardTransformConfigUpdate]

  implicit val errorOrUnitDecoder: Decoder[Either[CdpApiError, Unit]] =
    EitherDecoder.eitherDecoder[CdpApiError, Unit]
  implicit val cogniteExternalIdDecoder: Decoder[CogniteExternalId] =
    deriveDecoder[CogniteExternalId]
}
