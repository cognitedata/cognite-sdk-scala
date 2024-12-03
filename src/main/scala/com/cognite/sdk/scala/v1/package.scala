// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala

import com.cognite.sdk.scala.sttp.GzipBackend
import cats.Id
import _root_.sttp.client3.{EitherBackend, HttpURLConnectionBackend, SttpBackend}

package object v1 {
  type OrError[T] = Either[Throwable, T]
  implicit val sttpBackend: SttpBackend[OrError, Any] =
    new EitherBackend(new GzipBackend[Id, Any](HttpURLConnectionBackend()))
}
