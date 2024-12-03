// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala

import com.cognite.sdk.scala.sttp.GzipBackend
import _root_.sttp.client3.{EitherBackend, HttpURLConnectionBackend, SttpBackend}

package object v1 {
  type OrError[T] = Either[Throwable, T]
  implicit val sttpBackend: SttpBackend[OrError, Any] =
    new GzipBackend[OrError, Any](new EitherBackend(HttpURLConnectionBackend()))
}
