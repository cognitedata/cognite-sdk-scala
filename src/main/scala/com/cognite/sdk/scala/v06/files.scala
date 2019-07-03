package com.cognite.sdk.scala.v06

import com.cognite.sdk.scala.common.{Auth, ReadableResourceWithRetrieve, WithId}
import com.softwaremill.sttp._
import io.circe.generic.auto._

final case class File(
    id: Long = 0,
    fileName: String,
    directory: Option[String] = None,
    source: Option[String] = None,
    sourceId: Option[String] = None,
    fileType: Option[String] = None,
    metadata: Option[Map[String, String]] = None,
    assetIds: Option[Seq[Long]] = None,
    uploaded: Option[Boolean] = None,
    uploadedAt: Option[Long] = None,
    createdTime: Option[Long] = None,
    lastUpdatedTime: Option[Long] = None
) extends WithId[Long]

class Files[F[_]](project: String)(implicit auth: Auth)
    extends ReadableResourceWithRetrieve[File, F, Data, Long, Long]
    with ResourceV0_6[F] {
  override val baseUri = uri"https://api.cognitedata.com/api/0.6/projects/$project/files"
}
