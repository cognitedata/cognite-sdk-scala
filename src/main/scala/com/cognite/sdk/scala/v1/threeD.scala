package com.cognite.sdk.scala.v1

import com.cognite.sdk.scala.common.{Auth, CdpApiError, CogniteId, WithId}
import com.softwaremill.sttp._
import com.softwaremill.sttp.circe._
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
    extends ReadWritableResourceV1[ThreeDModel, CreateThreeDModel, F]
    with ResourceV1[F] {
  override val baseUri = uri"https://api.cognitedata.com/api/v1/projects/$project/3d/models"

  override def retrieveByIds(ids: Seq[Long]): F[Response[Seq[ThreeDModel]]] =
    ids match {
      case id :: Nil =>
        request
          .get(uri"$baseUri/$id")
          .response(asJson[Either[CdpApiError[CogniteId], ThreeDModel]])
          .mapResponse {
            case Left(value) => throw value.error
            case Right(Left(cdpApiError)) => throw cdpApiError.asException(uri"$baseUri/byids")
            case Right(Right(value)) => Seq(value)
          }
          .send()
      case _ => throw new RuntimeException("3D only support retrieving one 3D model per call")
    }
}
