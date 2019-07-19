package com.cognite.sdk.scala.v1.resources

import com.cognite.sdk.scala.common._
import com.cognite.sdk.scala.v1.{
  CreateThreeDModel,
  CreateThreeDRevision,
  ThreeDModel,
  ThreeDRevision
}
import com.softwaremill.sttp._
import com.softwaremill.sttp.circe._
import io.circe.generic.auto._
import io.circe.{Decoder, Encoder}

class ThreeDModels[F[_]](project: String)(implicit auth: Auth)
    extends ReadWritableResource[ThreeDModel, CreateThreeDModel, F, Id, CogniteId, Long] {
  override val baseUri = uri"https://api.cognitedata.com/api/v1/projects/$project/3d/models"

  implicit val errorOrUnitDecoder: Decoder[Either[CdpApiError, Unit]] =
    EitherDecoder.eitherDecoder[CdpApiError, Unit]
  def deleteByIds(ids: Seq[Long])(
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

class ThreeDRevisions[F[_]](project: String, modelId: Long)(
    implicit auth: Auth
) extends ReadWritableResource[
      ThreeDRevision,
      CreateThreeDRevision,
      F,
      Id,
      CogniteId,
      Long
    ]
    with RequestSession {
  override val baseUri =
    uri"https://api.cognitedata.com/api/v1/projects/$project/3d/models/$modelId/revisions"

  def deleteByIds(ids: Seq[Long])(
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
    with Readable[ThreeDAssetMapping, CreateThreeDAssetMapping, Id] {
  override val baseUri =
    uri"${requestSession.baseUri}/3d/models/$modelId/revisions/$revisionId/mappings"
}
