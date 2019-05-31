package com.cognite.sdk.scala.v1_0

import com.cognite.sdk.scala.common.{Items, WritableResource}
import com.softwaremill.sttp.circe._
import com.softwaremill.sttp._
import io.circe.generic.auto._

trait WritableResourceV1[R, W, F[_]] extends WritableResource[R, W, F, Id] {
  def deleteByExternalIds(externalIds: Seq[String]): F[Response[Unit]] =
    request
      .post(uri"$baseUri/delete")
      .body(Items(externalIds.map(CogniteExternalId)))
      .mapResponse(_ => ())
      .send()
}
