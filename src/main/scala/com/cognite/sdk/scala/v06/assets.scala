package com.cognite.sdk.scala.v06

import com.cognite.sdk.scala.common._
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
    sourceId: Option[String] = None,
    createdTime: Option[Long] = None,
    lastUpdatedTime: Option[Long] = None
) extends WithId[Long]

final case class CreateAsset(
    name: String,
    parentId: Option[Long] = None,
    description: Option[String] = None,
    source: Option[String] = None,
    sourceId: Option[String] = None,
    metadata: Option[Map[String, String]] = None
)

class Assets[F[_]](project: String)(implicit auth: Auth, sttpBackend: SttpBackend[F, _])
    extends ReadWritableResourceV0_6[Asset, CreateAsset, F]
    with RetrieveByIds[Asset, F, Data, Long, Long]
    with ResourceV0_6[F] {
  override val baseUri = uri"https://api.cognitedata.com/api/0.6/projects/$project/assets"
}
