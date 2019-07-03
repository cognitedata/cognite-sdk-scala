package com.cognite.sdk.scala.v1.resources

import com.cognite.sdk.scala.common.CogniteId

trait ResourceV1[F[_]] {
  def toInternalId(id: Long): CogniteId = CogniteId(id)
  //
}
