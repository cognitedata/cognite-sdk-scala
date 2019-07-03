package com.cognite.sdk.scala.v06.resources

import com.cognite.sdk.scala.common.Resource

trait ResourceV0_6[F[_]] extends Resource[F, Long, Long] {
  def toInternalId(id: Long): Long = id
  //implicit val idEncoder: Encoder[Long] = Encoder.encodeLong
}
