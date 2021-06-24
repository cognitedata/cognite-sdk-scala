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
    with Create[TransformConfigRead, TransformConfigCreate, F]
    with RetrieveByIdsWithIgnoreUnknownIds[TransformConfigRead, F]
    with Readable[TransformConfigRead, F]
    with RetrieveByExternalIdsWithIgnoreUnknownIds[TransformConfigRead, F]
    with DeleteByIdsWithIgnoreUnknownIds[F, Long]
    with DeleteByExternalIdsWithIgnoreUnknownIds[F]
    with UpdateById[TransformConfigRead, StandardTransformConfigUpdate, F]
    with UpdateByExternalId[TransformConfigRead, StandardTransformConfigUpdate, F] {
  import Transformations._
  override val baseUrl = uri"${requestSession.baseUrl}/transformations"

  override private[sdk] def readWithCursor(
      cursor: Option[String],
      limit: Option[Int],
      partition: Option[Partition]
  ): F[ItemsWithCursor[TransformConfigRead]] =
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
  ): F[Seq[TransformConfigRead]] =
    RetrieveByIdsWithIgnoreUnknownIds.retrieveByIds(
      requestSession,
      baseUrl,
      ids,
      ignoreUnknownIds
    )

  override def retrieveByExternalIds(
      externalIds: Seq[String],
      ignoreUnknownIds: Boolean
  ): F[Seq[TransformConfigRead]] =
    RetrieveByExternalIdsWithIgnoreUnknownIds.retrieveByExternalIds(
      requestSession,
      baseUrl,
      externalIds,
      ignoreUnknownIds
    )

  override def createItems(items: Items[TransformConfigCreate]): F[Seq[TransformConfigRead]] =
    Create.createItems[F, TransformConfigRead, TransformConfigCreate](requestSession, baseUrl, items)

  override def updateById(items: Map[Long, StandardTransformConfigUpdate]): F[Seq[TransformConfigRead]] =
    UpdateById.updateById[F, TransformConfigRead, StandardTransformConfigUpdate](requestSession, baseUrl, items)

  override def updateByExternalId(items: Map[String, StandardTransformConfigUpdate]): F[Seq[TransformConfigRead]] =
    UpdateByExternalId.updateByExternalId[F, TransformConfigRead, StandardTransformConfigUpdate](
      requestSession,
      baseUrl,
      items
    )

  override def deleteByIds(ids: Seq[Long]): F[Unit] = deleteByIds(ids, false)

  override def deleteByIds(ids: Seq[Long], ignoreUnknownIds: Boolean): F[Unit] =
    DeleteByIds.deleteByIdsWithIgnoreUnknownIds(requestSession, baseUrl, ids, ignoreUnknownIds)

  def deleteByIds(
      ids: Seq[Long],
      recursive: Boolean,
      ignoreUnknownIds: Boolean
  ): F[Unit] =
    requestSession.post[Unit, Unit, ItemsWithRecursiveAndIgnoreUnknownIds](
      ItemsWithRecursiveAndIgnoreUnknownIds(
        ids.map(CogniteInternalId.apply),
        recursive,
        ignoreUnknownIds
      ),
      uri"$baseUrl/delete",
      _ => ()
    )

  override def deleteByExternalIds(externalIds: Seq[String]): F[Unit] =
    deleteByExternalIds(externalIds, false)

  override def deleteByExternalIds(externalIds: Seq[String], ignoreUnknownIds: Boolean): F[Unit] =
    DeleteByExternalIds.deleteByExternalIdsWithIgnoreUnknownIds(
      requestSession,
      baseUrl,
      externalIds,
      ignoreUnknownIds
    )

  def deleteByExternalIds(
      externalIds: Seq[String],
      recursive: Boolean,
      ignoreUnknownIds: Boolean
  ): F[Unit] =
    requestSession.post[Unit, Unit, ItemsWithRecursiveAndIgnoreUnknownIds](
      ItemsWithRecursiveAndIgnoreUnknownIds(
        externalIds.map(CogniteExternalId.apply),
        recursive,
        ignoreUnknownIds
      ),
      uri"$baseUrl/delete",
      _ => ()
    )


  def query[I](
      query: String,
      limit: Int = 1000
  )(implicit itemDecoder: Decoder[I]): F[QueryResponse[I]] = {
    implicit val responseItemsDecoder: Decoder[Items[I]] = deriveDecoder[Items[I]]
    responseItemsDecoder.hashCode // suppress no usage warning... 🤦
    implicit val responseDecoder: Decoder[QueryResponse[I]] = deriveDecoder[QueryResponse[I]]
    requestSession.post[QueryResponse[I], QueryResponse[I], QueryQuery](
      QueryQuery(query),
      uri"$baseUrl/query/run".addParam("limit", limit.toString),
      identity
    )
  }

  def queryOne[I](
      q: String
  )(implicit itemDecoder: Decoder[I]): F[I] = {
    query[I](q, limit = 1).map(_.results.items.head)
  }
}

object Transformations {
  implicit val readDecoder: Decoder[TransformConfigRead] = deriveDecoder[TransformConfigRead]
  implicit val readItemsWithCursorDecoder: Decoder[ItemsWithCursor[TransformConfigRead]] =
    deriveDecoder[ItemsWithCursor[TransformConfigRead]]
  implicit val readItemsDecoder: Decoder[Items[TransformConfigRead]] =
    deriveDecoder[Items[TransformConfigRead]]
  implicit val createEncoder: Encoder[TransformConfigCreate] = deriveEncoder[TransformConfigCreate]
  implicit val createItemsEncoder: Encoder[Items[TransformConfigCreate]] = deriveEncoder[Items[TransformConfigCreate]]
  implicit val updateEncoder: Encoder[StandardTransformConfigUpdate] = deriveEncoder[StandardTransformConfigUpdate]

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
  implicit val querySchemaItemsDecoder: Decoder[Items[QuerySchemaColumn]] = deriveDecoder[Items[QuerySchemaColumn]]
}
