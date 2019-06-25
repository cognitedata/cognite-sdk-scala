package com.cognite.sdk.scala.v1

import com.cognite.sdk.scala.common.{
  Auth,
  CdpApiError,
  CogniteId,
  EitherDecoder,
  Items,
  ReadWritableResource,
  WithId
}
import com.softwaremill.sttp._
import com.softwaremill.sttp.circe._
import io.circe.Decoder
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

class ThreeDModels[F[_]](project: String)(implicit auth: Auth, sttpBackend: SttpBackend[F, _])
    extends ReadWritableResource[ThreeDModel, CreateThreeDModel, F, Id, CogniteId, Long]
    with ResourceV1[F] {
  override val baseUri = uri"https://api.cognitedata.com/api/v1/projects/$project/3d/models"

  implicit val errorOrUnitDecoder: Decoder[Either[CdpApiError[CogniteId], Unit]] =
    EitherDecoder.eitherDecoder[CdpApiError[CogniteId], Unit]
  def deleteByIds(ids: Seq[Long]): F[Response[Unit]] =
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
