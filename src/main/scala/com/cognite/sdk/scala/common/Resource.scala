// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.common

import cats.MonadError
import com.cognite.sdk.scala.v1.RequestSession
import natchez.Trace
import sttp.model.Uri

object Resource {
  val defaultLimit: Int = 1000
}

trait BaseUrl {
  val baseUrl: Uri
}

trait WithRequestSession[F[_]] {
  val requestSession: RequestSession[F]
  implicit val F: MonadError[F, Throwable] = requestSession.implicits.FMonad
  implicit val FTrace: Trace[F] = requestSession.implicits.FTrace
}
