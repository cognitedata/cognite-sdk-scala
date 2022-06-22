// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala

import cats.Id
import com.cognite.sdk.scala.sttp.GzipBackend
import _root_.sttp.client3.{HttpURLConnectionBackend, SttpBackend}

package object v1 {
  implicit val sttpBackend: SttpBackend[Id, Any] =
    new GzipBackend[Id, Any](HttpURLConnectionBackend())
}
