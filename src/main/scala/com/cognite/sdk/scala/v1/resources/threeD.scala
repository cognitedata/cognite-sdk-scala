package com.cognite.sdk.scala.v1.resources

import com.cognite.sdk.scala.common._
import com.cognite.sdk.scala.v1._
import com.softwaremill.sttp._
import com.softwaremill.sttp.circe._
import io.circe.derivation.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

class ThreeDModels[F[_]](val requestSession: RequestSession[F])
    extends Create[ThreeDModel, ThreeDModelCreate, F]
    with RetrieveByIds[ThreeDModel, F]
    with Readable[ThreeDModel, F]
    with DeleteByIds[F, Long]
    with UpdateById[ThreeDModel, ThreeDModelUpdate, F]
    with WithRequestSession[F] {
  import ThreeDModels._
  override val baseUri = uri"${requestSession.baseUri}/3d/models"

  override def deleteByIds(ids: Seq[Long]): F[Unit] = {
    implicit val errorOrUnitDecoder: Decoder[Either[CdpApiError, Unit]] =
      EitherDecoder.eitherDecoder[CdpApiError, Unit]
    // TODO: group deletes by max deletion request size
    //       or assert that length of `ids` is less than max deletion request size
    requestSession
      .sendCdf { request =>
        request
          .post(uri"$baseUri/delete")
          .body(Items(ids.map(CogniteInternalId)))
          .response(asJson[Either[CdpApiError, Unit]])
          .mapResponse {
            case Left(value) => throw value.error
            case Right(Left(cdpApiError)) => throw cdpApiError.asException(uri"$baseUri/delete")
            case Right(Right(_)) => ()
          }
      }
  }

  override private[sdk] def readWithCursor(
      cursor: Option[String],
      limit: Option[Int],
      partition: Option[Partition]
  ): F[ItemsWithCursor[ThreeDModel]] =
    Readable.readWithCursor(
      requestSession,
      baseUri,
      cursor,
      limit,
      None,
      Constants.defaultBatchSize
    )

  override def retrieveByIds(ids: Seq[Long]): F[Seq[ThreeDModel]] =
    RetrieveByIds.retrieveByIds(requestSession, baseUri, ids)

  override def createItems(items: Items[ThreeDModelCreate]): F[Seq[ThreeDModel]] =
    Create.createItems[F, ThreeDModel, ThreeDModelCreate](requestSession, baseUri, items)

  override def updateById(items: Map[Long, ThreeDModelUpdate]): F[Seq[ThreeDModel]] =
    UpdateById.updateById[F, ThreeDModel, ThreeDModelUpdate](requestSession, baseUri, items)
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
  override val baseUri =
    uri"${requestSession.baseUri}/3d/models/$modelId/revisions/$revisionId/nodes"
  override private[sdk] def readWithCursor(
      cursor: Option[String],
      limit: Option[Int],
      partition: Option[Partition]
  ): F[ItemsWithCursor[ThreeDNode]] =
    Readable.readWithCursor(
      requestSession,
      baseUri,
      cursor,
      limit,
      None,
      Constants.defaultBatchSize
    )

  override def retrieveByIds(ids: Seq[Long]): F[Seq[ThreeDNode]] =
    RetrieveByIds.retrieveByIds(requestSession, baseUri, ids)

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
  override val baseUri =
    uri"${requestSession.baseUri}/3d/models/$modelId/revisions/$revisionId/nodes/$nodeId/ancestors"
  override private[sdk] def readWithCursor(
      cursor: Option[String],
      limit: Option[Int],
      partition: Option[Partition]
  ): F[ItemsWithCursor[ThreeDNode]] =
    Readable.readWithCursor(
      requestSession,
      baseUri,
      cursor,
      limit,
      None,
      Constants.defaultBatchSize
    )
}

object ThreeDNodes {
  implicit val propertyCategoryEncoder: Encoder[PropertyCategory] = deriveEncoder[PropertyCategory]
  implicit val propertyCategoryDecoder: Decoder[PropertyCategory] = deriveDecoder[PropertyCategory]
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

class ThreeDRevisions[F[_]](val requestSession: RequestSession[F], modelId: Long)
    extends Create[ThreeDRevision, ThreeDRevisionCreate, F]
    with RetrieveByIds[ThreeDRevision, F]
    with Readable[ThreeDRevision, F]
    with DeleteByIds[F, Long]
    with UpdateById[ThreeDRevision, ThreeDRevisionUpdate, F]
    with WithRequestSession[F] {
  import ThreeDRevisions._
  override val baseUri =
    uri"${requestSession.baseUri}/3d/models/$modelId/revisions"

  override def deleteByIds(ids: Seq[Long]): F[Unit] = {
    implicit val errorOrUnitDecoder: Decoder[Either[CdpApiError, Unit]] =
      EitherDecoder.eitherDecoder[CdpApiError, Unit]
    // TODO: group deletes by max deletion request size
    //       or assert that length of `ids` is less than max deletion request size
    requestSession
      .sendCdf { request =>
        request
          .post(uri"$baseUri/delete")
          .body(Items(ids.map(CogniteInternalId)))
          .response(asJson[Either[CdpApiError, Unit]])
          .mapResponse {
            case Left(value) => throw value.error
            case Right(Left(cdpApiError)) => throw cdpApiError.asException(uri"$baseUri/delete")
            case Right(Right(_)) => ()
          }
      }
  }

  override private[sdk] def readWithCursor(
      cursor: Option[String],
      limit: Option[Int],
      partition: Option[Partition]
  ): F[ItemsWithCursor[ThreeDRevision]] =
    Readable.readWithCursor(
      requestSession,
      baseUri,
      cursor,
      limit,
      None,
      Constants.defaultBatchSize
    )

  override def retrieveByIds(ids: Seq[Long]): F[Seq[ThreeDRevision]] =
    RetrieveByIds.retrieveByIds(requestSession, baseUri, ids)

  override def createItems(items: Items[ThreeDRevisionCreate]): F[Seq[ThreeDRevision]] =
    Create.createItems[F, ThreeDRevision, ThreeDRevisionCreate](requestSession, baseUri, items)

  override def updateById(items: Map[Long, ThreeDRevisionUpdate]): F[Seq[ThreeDRevision]] =
    UpdateById.updateById[F, ThreeDRevision, ThreeDRevisionUpdate](requestSession, baseUri, items)
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
  override val baseUri =
    uri"${requestSession.baseUri}/3d/models/$modelId/revisions/$revisionId/mappings"

  override private[sdk] def readWithCursor(
      cursor: Option[String],
      limit: Option[Int],
      partition: Option[Partition]
  ): F[ItemsWithCursor[ThreeDAssetMapping]] =
    Readable.readWithCursor(
      requestSession,
      baseUri,
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
