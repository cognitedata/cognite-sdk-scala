// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala

import cats.Id
import sttp.client3.{HttpURLConnectionBackend, SttpBackend}
import com.cognite.sdk.scala.common.GzipSttpBackend

package object v1 {
  implicit val sttpBackend: SttpBackend[Id, Any] =
    new GzipSttpBackend[Id, Any](HttpURLConnectionBackend())
}
