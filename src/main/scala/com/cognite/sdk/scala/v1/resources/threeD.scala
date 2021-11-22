// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1.resources

import com.cognite.sdk.scala.common._
import com.cognite.sdk.scala.v1._
import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker
import sttp.client3._
import sttp.client3.jsoniter_scala._
import sttp.monad.MonadError
import sttp.monad.syntax._

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
  implicit val threeDModelCodec: JsonValueCodec[ThreeDModel] = JsonCodecMaker.make[ThreeDModel]
  implicit val threeDModelUpdateCodec: JsonValueCodec[ThreeDModelUpdate] =
    JsonCodecMaker.make[ThreeDModelUpdate]
  implicit val threeDModelItemsCodec: JsonValueCodec[Items[ThreeDModel]] =
    JsonCodecMaker.make[Items[ThreeDModel]]
  implicit val threeDModelItemsWithCursorCodec: JsonValueCodec[ItemsWithCursor[ThreeDModel]] =
    JsonCodecMaker.make[ItemsWithCursor[ThreeDModel]]
  implicit val createThreeDModelCodec: JsonValueCodec[ThreeDModelCreate] =
    JsonCodecMaker.make[ThreeDModelCreate]
  implicit val createThreeDModelItemsCodec: JsonValueCodec[Items[ThreeDModelCreate]] =
    JsonCodecMaker.make[Items[ThreeDModelCreate]]
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
  implicit val propertiesCodec: JsonValueCodec[Properties] = JsonCodecMaker.make[Properties]
  implicit val boundingBoxCodec: JsonValueCodec[BoundingBox] = JsonCodecMaker.make[BoundingBox]
  implicit val threeDNodeCodec: JsonValueCodec[ThreeDNode] = JsonCodecMaker.make[ThreeDNode]
  implicit val threeDNodeItemsWithCursorCodec: JsonValueCodec[ItemsWithCursor[ThreeDNode]] =
    JsonCodecMaker.make[ItemsWithCursor[ThreeDNode]]
  implicit val threeDNodeItemsCodec: JsonValueCodec[Items[ThreeDNode]] = JsonCodecMaker.make[Items[ThreeDNode]]
}

class ThreeDRevisions[F[_]: MonadError](val requestSession: RequestSession[F], modelId: Long)
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

  override def retrieveByIds(ids: Seq[Long]): F[Seq[ThreeDRevision]] =
    ids.foldRight(List.empty[ThreeDRevision].unit) { (id, acc: F[List[ThreeDRevision]]) =>
      retrieveById(id).flatMap { revision =>
        acc.map(revisions => revision :: revisions)
      }
    }.map(_.toSeq)

  override def createItems(items: Items[ThreeDRevisionCreate]): F[Seq[ThreeDRevision]] =
    Create.createItems[F, ThreeDRevision, ThreeDRevisionCreate](requestSession, baseUrl, items)

  override def updateById(items: Map[Long, ThreeDRevisionUpdate]): F[Seq[ThreeDRevision]] =
    UpdateById.updateById[F, ThreeDRevision, ThreeDRevisionUpdate](requestSession, baseUrl, items)
}

object ThreeDRevisions {
  implicit val threeDRevisionCameraCodec: JsonValueCodec[Camera] = JsonCodecMaker.make[Camera]
  implicit val threeDRevisionCodec: JsonValueCodec[ThreeDRevision] = JsonCodecMaker.make[ThreeDRevision]
  implicit val threeDRevisionUpdateCodec: JsonValueCodec[ThreeDRevisionUpdate] =
    JsonCodecMaker.make[ThreeDRevisionUpdate]
  implicit val threeDRevisionItemsCodec: JsonValueCodec[Items[ThreeDRevision]] =
    JsonCodecMaker.make[Items[ThreeDRevision]]
  implicit val threeDRevisionItemsWithCursorCodec: JsonValueCodec[ItemsWithCursor[ThreeDRevision]] =
    JsonCodecMaker.make[ItemsWithCursor[ThreeDRevision]]
  implicit val createThreeDRevisionCodec: JsonValueCodec[ThreeDRevisionCreate] =
    JsonCodecMaker.make[ThreeDRevisionCreate]
  implicit val createThreeDRevisionItemsCodec: JsonValueCodec[Items[ThreeDRevisionCreate]] =
    JsonCodecMaker.make[Items[ThreeDRevisionCreate]]
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
  implicit val threeDAssetMappingCodec: JsonValueCodec[ThreeDAssetMapping] =
    JsonCodecMaker.make[ThreeDAssetMapping]
  implicit val threeDAssetMappingItemsWithCursorCodec
      : JsonValueCodec[ItemsWithCursor[ThreeDAssetMapping]] =
    JsonCodecMaker.make[ItemsWithCursor[ThreeDAssetMapping]]
}
