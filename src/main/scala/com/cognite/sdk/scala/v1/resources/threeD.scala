package com.cognite.sdk.scala.v1.resources

import com.cognite.sdk.scala.common._
import com.softwaremill.sttp._
import com.softwaremill.sttp.circe._
import io.circe.{Decoder, Encoder}
import io.circe.generic.auto._

final case class ThreeDModel(
    name: String,
    id: Long = 0,
    createdTime: Option[Long] = None,
    metadata: Option[Map[String, String]] = None
) extends WithId[Long]

final case class CreateThreeDModel(
    name: String,
    metadata: Option[Map[String, String]] = None
)

class ThreeDModels[F[_]](project: String)(implicit auth: Auth)
    extends ReadWritableResource[ThreeDModel, CreateThreeDModel, F, Id, CogniteId, Long]
    with ResourceV1[F] {
  override val baseUri = uri"https://api.cognitedata.com/api/v1/projects/$project/3d/models"

  implicit val errorOrUnitDecoder: Decoder[Either[CdpApiError[CogniteId], Unit]] =
    EitherDecoder.eitherDecoder[CdpApiError[CogniteId], Unit]
  def deleteByIds(ids: Seq[Long])(
      implicit sttpBackend: SttpBackend[F, _],
      errorDecoder: Decoder[CdpApiError[CogniteId]],
      itemsEncoder: Encoder[Items[CogniteId]]
  ): F[Response[Unit]] =
    // TODO: group deletes by max deletion request size
    //       or assert that length of `ids` is less than max deletion request size
    request
      .post(uri"$baseUri/delete")
      .body(Items(ids.map(toInternalId)))
      .response(asJson[Either[CdpApiError[CogniteId], Unit]])
      .mapResponse {
        case Left(value) => throw value.error
        case Right(Left(cdpApiError)) => throw cdpApiError.asException(uri"$baseUri/delete")
        case Right(Right(_)) => ()
      }
      .send()
}

final case class Camera(
    target: Option[Array[Double]],
    position: Option[Array[Double]]
)

final case class ThreeDRevision(
    id: Long,
    fileId: Long,
    published: Boolean,
    rotation: Option[Array[Double]] = None,
    camera: Option[Camera] = None,
    status: String,
    metadata: Option[Map[String, String]] = None,
    thumbnailThreedFileId: Option[Long] = None,
    thumbnailURL: Option[String] = None,
    assetMappingCount: Long,
    createdTime: Long
) extends WithId[Long]

final case class CreateThreeDRevision(
    published: Boolean,
    rotation: Option[Array[Double]] = None,
    metadata: Option[Map[String, String]] = None,
    camera: Option[Camera] = None,
    fileId: Long
)

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
    with ResourceV1[F] {
  override val baseUri =
    uri"https://api.cognitedata.com/api/v1/projects/$project/3d/models/$modelId/revisions"

  implicit val errorOrUnitDecoder: Decoder[Either[CdpApiError[CogniteId], Unit]] =
    EitherDecoder.eitherDecoder[CdpApiError[CogniteId], Unit]
  def deleteByIds(ids: Seq[Long])(
      implicit sttpBackend: SttpBackend[F, _],
      errorDecoder: Decoder[CdpApiError[CogniteId]],
      itemsEncoder: Encoder[Items[CogniteId]]
  ): F[Response[Unit]] =
    // TODO: group deletes by max deletion request size
    //       or assert that length of `ids` is less than max deletion request size
    request
      .post(uri"$baseUri/delete")
      .body(Items(ids.map(toInternalId)))
      .response(asJson[Either[CdpApiError[CogniteId], Unit]])
      .mapResponse {
        case Left(value) => throw value.error
        case Right(Left(cdpApiError)) => throw cdpApiError.asException(uri"$baseUri/delete")
        case Right(Right(_)) => ()
      }
      .send()
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

class ThreeDAssetMappings[F[_]](project: String, modelId: Long, revisionId: Long)(
    implicit auth: Auth
) extends ReadableResource[
      ThreeDAssetMapping,
      F,
      Id,
      CogniteId,
      Long
    ]
    with ResourceV1[F] {
  override val baseUri =
    uri"https://api.cognitedata.com/api/v1/projects/$project/3d/models/$modelId/revisions/$revisionId/mappings"
}
