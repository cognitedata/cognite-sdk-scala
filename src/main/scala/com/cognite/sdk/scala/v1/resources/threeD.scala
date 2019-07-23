package com.cognite.sdk.scala.v1.resources

import com.cognite.sdk.scala.common._
import com.cognite.sdk.scala.v1.{CreateThreeDModel, CreateThreeDRevision, ThreeDModel, ThreeDModelUpdate, ThreeDRevision, ThreeDRevisionUpdate}
import com.softwaremill.sttp._
import com.softwaremill.sttp.circe._
import io.circe.generic.auto._
import io.circe.{Decoder, Encoder}

class ThreeDModels[F[_]](val requestSession: RequestSession)
    extends Create[ThreeDModel, CreateThreeDModel, F, Id]
    with RetrieveByIds[ThreeDModel, F, Id]
    with Readable[ThreeDModel, F, Id]
    with DeleteByIdsV1[ThreeDModel, CreateThreeDModel, F, Id]
    with Update[ThreeDModel, ThreeDModelUpdate, F, Id]
    with WithRequestSession {
  override val baseUri = uri"${requestSession.baseUri}/3d/models"

  implicit val errorOrUnitDecoder: Decoder[Either[CdpApiError, Unit]] =
    EitherDecoder.eitherDecoder[CdpApiError, Unit]
  override def deleteByIds(ids: Seq[Long])(
      implicit sttpBackend: SttpBackend[F, _],
      auth: Auth,
      errorDecoder: Decoder[CdpApiError],
      itemsEncoder: Encoder[Items[CogniteId]]
  ): F[Response[Unit]] = {
    implicit val errorOrUnitDecoder: Decoder[Either[CdpApiError, Unit]] =
      EitherDecoder.eitherDecoder[CdpApiError, Unit]
    // TODO: group deletes by max deletion request size
    //       or assert that length of `ids` is less than max deletion request size
    requestSession.request
      .post(uri"$baseUri/delete")
      .body(Items(ids.map(CogniteId)))
      .response(asJson[Either[CdpApiError, Unit]])
      .mapResponse {
        case Left(value) => throw value.error
        case Right(Left(cdpApiError)) => throw cdpApiError.asException(uri"$baseUri/delete")
        case Right(Right(_)) => ()
      }
      .send()
  }

}

class ThreeDRevisions[F[_]](val requestSession: RequestSession, modelId: Long)
    extends Create[ThreeDRevision, CreateThreeDRevision, F, Id]
    with RetrieveByIds[ThreeDRevision, F, Id]
    with Readable[ThreeDRevision, F, Id]
    with DeleteByIdsV1[ThreeDRevision, CreateThreeDRevision, F, Id]
    with Update[ThreeDRevision, ThreeDRevisionUpdate, F, Id]
    with WithRequestSession {
  override val baseUri =
    uri"${requestSession.baseUri}/3d/models/$modelId/revisions"

  override def deleteByIds(ids: Seq[Long])(
      implicit sttpBackend: SttpBackend[F, _],
      auth: Auth,
      errorDecoder: Decoder[CdpApiError],
      itemsEncoder: Encoder[Items[CogniteId]]
  ): F[Response[Unit]] = {
    implicit val errorOrUnitDecoder: Decoder[Either[CdpApiError, Unit]] =
      EitherDecoder.eitherDecoder[CdpApiError, Unit]
    // TODO: group deletes by max deletion request size
    //       or assert that length of `ids` is less than max deletion request size
    requestSession.request
      .post(uri"$baseUri/delete")
      .body(Items(ids.map(CogniteId)))
      .response(asJson[Either[CdpApiError, Unit]])
      .mapResponse {
        case Left(value) => throw value.error
        case Right(Left(cdpApiError)) => throw cdpApiError.asException(uri"$baseUri/delete")
        case Right(Right(_)) => ()
      }
      .send()
  }

}

final case class ThreeDAssetMapping(
    nodeId: Long,
    assetId: Long,
    treeIndex: Option[Long] = None,
    subtreeSize: Option[Long] = None
)

final case class CreateThreeDAssetMapping(
    nodeId: Long,
    assetId: Long
)

class ThreeDAssetMappings[F[_]](val requestSession: RequestSession, modelId: Long, revisionId: Long)
    extends WithRequestSession
    with Readable[ThreeDAssetMapping, F, Id] {
  override val baseUri =
    uri"${requestSession.baseUri}/3d/models/$modelId/revisions/$revisionId/mappings"
}
