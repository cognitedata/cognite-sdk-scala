package com.cognite.sdk.scala.v1_0

import com.cognite.sdk.scala.common.{CogniteId, ReadableResource}
import com.softwaremill.sttp.Id

trait ReadableResourceV1[R, F[_]] extends ReadableResource[R, F, Id, CogniteId] {
  //def retrieveByExternalIds(externalIds: List[String])
}
