// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1.resources

import cats.Applicative
import com.cognite.sdk.scala.common._
import com.cognite.sdk.scala.v1._
import sttp.client3._
import sttp.client3.circe._
import cats.implicits._
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

class ThreeDModels[F[_]](val requestSession: RequestSession[F])
    extends Create[ThreeDModel, ThreeDModelCreate, F]
    with RetrieveByIds[ThreeDModel, F]
    with Readable[ThreeDModel, F]
    with DeleteByIds[F, Long]
    with UpdateById[ThreeDModel, ThreeDModelUpdate, F]
    with WithRequestSession[F] {
  import ThreeDModels._
  override val baseUrl = uri"${requestSession.baseUrl}/3d/models"

  override def deleteByIds(ids: Seq[Long]): F[Unit] =
    // TODO: group deletes by max deletion request size
    //       or assert that length of `ids` is less than max deletion request size
    requestSession
      .post[Unit, Unit, Items[CogniteInternalId]](
        Items(ids.map(CogniteInternalId.apply)),
        uri"$baseUrl/delete",
        _ => ()
      )

  override private[sdk] def readWithCursor(
      cursor: Option[String],
      limit: Option[Int],
      partition: Option[Partition]
  ): F[ItemsWithCursor[ThreeDModel]] =
    Readable.readWithCursor(
      requestSession,
      baseUrl,
      cursor,
      limit,
      None,
      Constants.defaultBatchSize
    )

  override def retrieveByIds(ids: Seq[Long]): F[Seq[ThreeDModel]] =
    RetrieveByIds.retrieveByIds(requestSession, baseUrl, ids)

  override def createItems(items: Items[ThreeDModelCreate]): F[Seq[ThreeDModel]] =
    Create.createItems[F, ThreeDModel, ThreeDModelCreate](requestSession, baseUrl, items)

  override def updateById(items: Map[Long, ThreeDModelUpdate]): F[Seq[ThreeDModel]] =
    UpdateById.updateById[F, ThreeDModel, ThreeDModelUpdate](requestSession, baseUrl, items)
}

object ThreeDModels {
  implicit val threeDModelDecoder: Decoder[ThreeDModel] = deriveDecoder[ThreeDModel]
  implicit val threeDModelUpdateEncoder: Encoder[ThreeDModelUpdate] =
    deriveEncoder[ThreeDModelUpdate]
  implicit val threeDModelItemsDecoder: Decoder[Items[ThreeDModel]] =
    deriveDecoder[Items[ThreeDModel]]
  implicit val threeDModelItemsWithCursorDecoder: Decoder[ItemsWithCursor[ThreeDModel]] =
    deriveDecoder[ItemsWithCursor[ThreeDModel]]
  // WartRemover gets confused by circe-derivation
  @SuppressWarnings(Array("org.wartremover.warts.JavaSerializable"))
  implicit val createThreeDModelDecoder: Decoder[ThreeDModelCreate] =
    deriveDecoder[ThreeDModelCreate]
  implicit val createThreeDModelEncoder: Encoder[ThreeDModelCreate] =
    deriveEncoder[ThreeDModelCreate]
  implicit val createThreeDModelItemsEncoder: Encoder[Items[ThreeDModelCreate]] =
    deriveEncoder[Items[ThreeDModelCreate]]
}

class ThreeDNodes[F[_]](val requestSession: RequestSession[F], modelId: Long, revisionId: Long)
    extends Readable[ThreeDNode, F]
    with RetrieveByIds[ThreeDNode, F]
    with WithRequestSession[F] {
  import ThreeDNodes._
  override val baseUrl =
    uri"${requestSession.baseUrl}/3d/models/$modelId/revisions/$revisionId/nodes"
  override private[sdk] def readWithCursor(
      cursor: Option[String],
      limit: Option[Int],
      partition: Option[Partition]
  ): F[ItemsWithCursor[ThreeDNode]] =
    Readable.readWithCursor(
      requestSession,
      baseUrl,
      cursor,
      limit,
      None,
      Constants.defaultBatchSize
    )

  override def retrieveByIds(ids: Seq[Long]): F[Seq[ThreeDNode]] =
    RetrieveByIds.retrieveByIds(requestSession, baseUrl, ids)

  def ancestors(nodeId: Long): ThreeDAncestorNodes[F] =
    new ThreeDAncestorNodes(requestSession, modelId, revisionId, nodeId)
}

class ThreeDAncestorNodes[F[_]](
    val requestSession: RequestSession[F],
    modelId: Long,
    revisionId: Long,
    nodeId: Long
) extends Readable[ThreeDNode, F]
    with WithRequestSession[F] {
  import ThreeDNodes._
  override val baseUrl =
    uri"${requestSession.baseUrl}/3d/models/$modelId/revisions/$revisionId/nodes/$nodeId/ancestors"
  override private[sdk] def readWithCursor(
      cursor: Option[String],
      limit: Option[Int],
      partition: Option[Partition]
  ): F[ItemsWithCursor[ThreeDNode]] =
    Readable.readWithCursor(
      requestSession,
      baseUrl,
      cursor,
      limit,
      None,
      Constants.defaultBatchSize
    )
}

object ThreeDNodes {
  implicit val propertiesEncoder: Encoder[Properties] = deriveEncoder[Properties]
  implicit val boundingBoxEncoder: Encoder[BoundingBox] = deriveEncoder[BoundingBox]
  implicit val propertiesDecoder: Decoder[Properties] = deriveDecoder[Properties]
  implicit val boundingBoxDecoder: Decoder[BoundingBox] = deriveDecoder[BoundingBox]
  implicit val threeDNodeEncoder: Encoder[ThreeDNode] = deriveEncoder[ThreeDNode]
  implicit val threeDNodeDecoder: Decoder[ThreeDNode] = deriveDecoder[ThreeDNode]
  implicit val threeDNodeItemsWithCursorDecoder: Decoder[ItemsWithCursor[ThreeDNode]] =
    deriveDecoder[ItemsWithCursor[ThreeDNode]]
  implicit val threeDNodeItemsEncoder: Encoder[Items[ThreeDNode]] = deriveEncoder[Items[ThreeDNode]]
  implicit val threeDNodeItemsDecoder: Decoder[Items[ThreeDNode]] = deriveDecoder[Items[ThreeDNode]]
}

class ThreeDRevisions[F[_]: Applicative](val requestSession: RequestSession[F], modelId: Long)
    extends Create[ThreeDRevision, ThreeDRevisionCreate, F]
    with RetrieveByIds[ThreeDRevision, F]
    with Readable[ThreeDRevision, F]
    with DeleteByIds[F, Long]
    with UpdateById[ThreeDRevision, ThreeDRevisionUpdate, F]
    with WithRequestSession[F] {
  import ThreeDRevisions._
  override val baseUrl =
    uri"${requestSession.baseUrl}/3d/models/$modelId/revisions"

  override def deleteByIds(ids: Seq[Long]): F[Unit] =
    // TODO: group deletes by max deletion request size
    //       or assert that length of `ids` is less than max deletion request size
    requestSession
      .post[Unit, Unit, Items[CogniteInternalId]](
        Items(ids.map(CogniteInternalId.apply)),
        uri"$baseUrl/delete",
        _ => ()
      )

  override private[sdk] def readWithCursor(
      cursor: Option[String],
      limit: Option[Int],
      partition: Option[Partition]
  ): F[ItemsWithCursor[ThreeDRevision]] =
    Readable.readWithCursor(
      requestSession,
      baseUrl,
      cursor,
      limit,
      None,
      Constants.defaultBatchSize
    )

  override def retrieveById(id: Long): F[ThreeDRevision] =
    requestSession.get[ThreeDRevision, ThreeDRevision](uri"$baseUrl/$id", value => value)

  // toSeq is redundant on Scala 2.13, not Scala 2.12.
  @SuppressWarnings(Array("org.wartremover.warts.RedundantConversions"))
  override def retrieveByIds(ids: Seq[Long]): F[Seq[ThreeDRevision]] =
    ids.toList.traverse(retrieveById).map(_.toSeq)

  override def createItems(items: Items[ThreeDRevisionCreate]): F[Seq[ThreeDRevision]] =
    Create.createItems[F, ThreeDRevision, ThreeDRevisionCreate](requestSession, baseUrl, items)

  override def updateById(items: Map[Long, ThreeDRevisionUpdate]): F[Seq[ThreeDRevision]] =
    UpdateById.updateById[F, ThreeDRevision, ThreeDRevisionUpdate](requestSession, baseUrl, items)
}

object ThreeDRevisions {
  implicit val threeDRevisionCameraDecoder: Decoder[Camera] = deriveDecoder[Camera]
  implicit val threeDRevisionCameraEncoder: Encoder[Camera] = deriveEncoder[Camera]
  implicit val threeDRevisionDecoder: Decoder[ThreeDRevision] = deriveDecoder[ThreeDRevision]
  implicit val threeDRevisionUpdateEncoder: Encoder[ThreeDRevisionUpdate] =
    deriveEncoder[ThreeDRevisionUpdate]
  implicit val threeDRevisionItemsDecoder: Decoder[Items[ThreeDRevision]] =
    deriveDecoder[Items[ThreeDRevision]]
  implicit val threeDRevisionItemsWithCursorDecoder: Decoder[ItemsWithCursor[ThreeDRevision]] =
    deriveDecoder[ItemsWithCursor[ThreeDRevision]]
  implicit val createThreeDRevisionDecoder: Decoder[ThreeDRevisionCreate] =
    deriveDecoder[ThreeDRevisionCreate]
  implicit val createThreeDRevisionEncoder: Encoder[ThreeDRevisionCreate] =
    deriveEncoder[ThreeDRevisionCreate]
  implicit val createThreeDRevisionItemsEncoder: Encoder[Items[ThreeDRevisionCreate]] =
    deriveEncoder[Items[ThreeDRevisionCreate]]
}

class ThreeDAssetMappings[F[_]](
    val requestSession: RequestSession[F],
    modelId: Long,
    revisionId: Long
) extends WithRequestSession[F]
    with Readable[ThreeDAssetMapping, F] {
  import ThreeDAssetMappings._
  override val baseUrl =
    uri"${requestSession.baseUrl}/3d/models/$modelId/revisions/$revisionId/mappings"

  override private[sdk] def readWithCursor(
      cursor: Option[String],
      limit: Option[Int],
      partition: Option[Partition]
  ): F[ItemsWithCursor[ThreeDAssetMapping]] =
    Readable.readWithCursor(
      requestSession,
      baseUrl,
      cursor,
      limit,
      None,
      Constants.defaultBatchSize
    )
}

object ThreeDAssetMappings {
  implicit val threeDAssetMappingDecoder: Decoder[ThreeDAssetMapping] =
    deriveDecoder[ThreeDAssetMapping]
  implicit val threeDAssetMappingItemsWithCursorDecoder
      : Decoder[ItemsWithCursor[ThreeDAssetMapping]] =
    deriveDecoder[ItemsWithCursor[ThreeDAssetMapping]]
}
