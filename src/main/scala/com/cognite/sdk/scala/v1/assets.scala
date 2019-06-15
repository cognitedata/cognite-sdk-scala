package com.cognite.sdk.scala.v1

import com.cognite.sdk.scala.common.{Auth, WithId}
import com.softwaremill.sttp._
import io.circe.generic.auto._

final case class Asset(
    id: Long = 0,
    path: Option[Seq[Long]] = None,
    depth: Option[Long] = None,
    name: String,
    parentId: Option[Long] = None,
    description: Option[String] = None,
    metadata: Option[Map[String, String]] = None,
    source: Option[String] = None,
    externalId: Option[String] = None,
    createdTime: Option[Long] = None,
    lastUpdatedTime: Option[Long] = None
) extends WithId[Long]

final case class CreateAsset(
    name: String,
    parentId: Option[Long] = None,
    description: Option[String] = None,
    source: Option[String] = None,
    externalId: Option[String] = None,
    metadata: Option[Map[String, String]] = None
)

class Assets[F[_]](project: String)(implicit auth: Auth, sttpBackend: SttpBackend[F, _])
    extends ReadWritableResourceV1[Asset, CreateAsset, F]
    with ResourceV1[F] {
  override val baseUri = uri"https://api.cognitedata.com/api/v1/projects/$project/assets"
}
