package com.cognite.sdk.scala.v1.resources

import com.cognite.sdk.scala.common._
import com.cognite.sdk.scala.v1._
import com.softwaremill.sttp._
import com.softwaremill.sttp.circe._
import io.circe.derivation.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

class ThreeDModels[F[_]](val requestSession: RequestSession[F])
    extends Create[ThreeDModel, CreateThreeDModel, F]
    with RetrieveByIds[ThreeDModel, F]
    with Readable[ThreeDModel, F]
    with DeleteByIds[F, Long]
    with Update[ThreeDModel, ThreeDModelUpdate, F]
    with WithRequestSession[F] {
  import ThreeDModels._
  override val baseUri = uri"${requestSession.baseUri}/3d/models"

  override def deleteByIds(ids: Seq[Long]): F[Response[Unit]] = {
    implicit val errorOrUnitDecoder: Decoder[Either[CdpApiError, Unit]] =
      EitherDecoder.eitherDecoder[CdpApiError, Unit]
    // TODO: group deletes by max deletion request size
    //       or assert that length of `ids` is less than max deletion request size
    requestSession
      .send { request =>
        request
          .post(uri"$baseUri/delete")
          .body(Items(ids.map(CogniteId)))
          .response(asJson[Either[CdpApiError, Unit]])
          .mapResponse {
            case Left(value) => throw value.error
            case Right(Left(cdpApiError)) => throw cdpApiError.asException(uri"$baseUri/delete")
            case Right(Right(_)) => ()
          }
      }
  }

  override def readWithCursor(
      cursor: Option[String],
      limit: Option[Long]
  ): F[Response[ItemsWithCursor[ThreeDModel]]] =
    Readable.readWithCursor(requestSession, baseUri, cursor, limit)

  override def retrieveByIds(ids: Seq[Long]): F[Response[Seq[ThreeDModel]]] =
    RetrieveByIds.retrieveByIds(requestSession, baseUri, ids)

  override def createItems(items: Items[CreateThreeDModel]): F[Response[Seq[ThreeDModel]]] =
    Create.createItems[F, ThreeDModel, CreateThreeDModel](requestSession, baseUri, items)

  override def updateItems(items: Seq[ThreeDModelUpdate]): F[Response[Seq[ThreeDModel]]] =
    Update.updateItems[F, ThreeDModel, ThreeDModelUpdate](requestSession, baseUri, items)
}

object ThreeDModels {
  implicit val cogniteIdEncoder: Encoder[CogniteId] = deriveEncoder
  implicit val cogniteIdItemsEncoder: Encoder[Items[CogniteId]] = deriveEncoder
  implicit val threeDModelDecoder: Decoder[ThreeDModel] = deriveDecoder[ThreeDModel]
  implicit val threeDModelUpdateEncoder: Encoder[ThreeDModelUpdate] =
    deriveEncoder[ThreeDModelUpdate]
  implicit val threeDModelItemsDecoder: Decoder[Items[ThreeDModel]] =
    deriveDecoder[Items[ThreeDModel]]
  implicit val threeDModelItemsWithCursorDecoder: Decoder[ItemsWithCursor[ThreeDModel]] =
    deriveDecoder[ItemsWithCursor[ThreeDModel]]
  // WartRemover gets confused by circe-derivation
  @SuppressWarnings(Array("org.wartremover.warts.JavaSerializable"))
  implicit val createThreeDModelDecoder: Decoder[CreateThreeDModel] =
    deriveDecoder[CreateThreeDModel]
  implicit val createThreeDModelEncoder: Encoder[CreateThreeDModel] =
    deriveEncoder[CreateThreeDModel]
  implicit val createThreeDModelItemsEncoder: Encoder[Items[CreateThreeDModel]] =
    deriveEncoder[Items[CreateThreeDModel]]
}

class ThreeDRevisions[F[_]](val requestSession: RequestSession[F], modelId: Long)
    extends Create[ThreeDRevision, CreateThreeDRevision, F]
    with RetrieveByIds[ThreeDRevision, F]
    with Readable[ThreeDRevision, F]
    with DeleteByIds[F, Long]
    with Update[ThreeDRevision, ThreeDRevisionUpdate, F]
    with WithRequestSession[F] {
  import ThreeDRevisions._
  override val baseUri =
    uri"${requestSession.baseUri}/3d/models/$modelId/revisions"

  override def deleteByIds(ids: Seq[Long]): F[Response[Unit]] = {
    implicit val errorOrUnitDecoder: Decoder[Either[CdpApiError, Unit]] =
      EitherDecoder.eitherDecoder[CdpApiError, Unit]
    // TODO: group deletes by max deletion request size
    //       or assert that length of `ids` is less than max deletion request size
    requestSession
      .send { request =>
        request
          .post(uri"$baseUri/delete")
          .body(Items(ids.map(CogniteId)))
          .response(asJson[Either[CdpApiError, Unit]])
          .mapResponse {
            case Left(value) => throw value.error
            case Right(Left(cdpApiError)) => throw cdpApiError.asException(uri"$baseUri/delete")
            case Right(Right(_)) => ()
          }
      }
  }

  override def readWithCursor(
      cursor: Option[String],
      limit: Option[Long]
  ): F[Response[ItemsWithCursor[ThreeDRevision]]] =
    Readable.readWithCursor(requestSession, baseUri, cursor, limit)

  override def retrieveByIds(ids: Seq[Long]): F[Response[Seq[ThreeDRevision]]] =
    RetrieveByIds.retrieveByIds(requestSession, baseUri, ids)

  override def createItems(items: Items[CreateThreeDRevision]): F[Response[Seq[ThreeDRevision]]] =
    Create.createItems[F, ThreeDRevision, CreateThreeDRevision](requestSession, baseUri, items)

  override def updateItems(items: Seq[ThreeDRevisionUpdate]): F[Response[Seq[ThreeDRevision]]] =
    Update.updateItems[F, ThreeDRevision, ThreeDRevisionUpdate](requestSession, baseUri, items)
}

object ThreeDRevisions {
  implicit val cogniteIdEncoder: Encoder[CogniteId] = deriveEncoder
  implicit val cogniteIdItemsEncoder: Encoder[Items[CogniteId]] = deriveEncoder
  implicit val threeDRevisionCameraDecoder: Decoder[Camera] = deriveDecoder[Camera]
  implicit val threeDRevisionCameraEncoder: Encoder[Camera] = deriveEncoder[Camera]
  implicit val threeDRevisionDecoder: Decoder[ThreeDRevision] = deriveDecoder[ThreeDRevision]
  implicit val threeDRevisionUpdateEncoder: Encoder[ThreeDRevisionUpdate] =
    deriveEncoder[ThreeDRevisionUpdate]
  implicit val threeDRevisionItemsDecoder: Decoder[Items[ThreeDRevision]] =
    deriveDecoder[Items[ThreeDRevision]]
  implicit val threeDRevisionItemsWithCursorDecoder: Decoder[ItemsWithCursor[ThreeDRevision]] =
    deriveDecoder[ItemsWithCursor[ThreeDRevision]]
  implicit val createThreeDRevisionDecoder: Decoder[CreateThreeDRevision] =
    deriveDecoder[CreateThreeDRevision]
  implicit val createThreeDRevisionEncoder: Encoder[CreateThreeDRevision] =
    deriveEncoder[CreateThreeDRevision]
  implicit val createThreeDRevisionItemsEncoder: Encoder[Items[CreateThreeDRevision]] =
    deriveEncoder[Items[CreateThreeDRevision]]
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

  override def readWithCursor(
      cursor: Option[String],
      limit: Option[Long]
  ): F[Response[ItemsWithCursor[ThreeDAssetMapping]]] =
    Readable.readWithCursor(requestSession, baseUri, cursor, limit)
}

object ThreeDAssetMappings {
  implicit val threeDAssetMappingDecoder: Decoder[ThreeDAssetMapping] =
    deriveDecoder[ThreeDAssetMapping]
  implicit val threeDAssetMappingItemsWithCursorDecoder
      : Decoder[ItemsWithCursor[ThreeDAssetMapping]] =
    deriveDecoder[ItemsWithCursor[ThreeDAssetMapping]]
}
