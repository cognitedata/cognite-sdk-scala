package com.cognite.sdk.scala.v1_0

import com.cognite.sdk.scala.common.ReadableResource
import com.softwaremill.sttp.Id

trait ReadableResourceV1[R, F[_]] extends ReadableResource[R, F, Id] {
  //def retrieveByExternalIds(externalIds: List[String])
}
