// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.common

import com.cognite.sdk.scala.v1.RequestSession
import com.softwaremill.sttp.Uri

object Resource {
  val defaultLimit: Int = 1000
}

trait BaseUrl {
  val baseUrl: Uri
}

trait WithRequestSession[F[_]] {
  val requestSession: RequestSession[F]
}
