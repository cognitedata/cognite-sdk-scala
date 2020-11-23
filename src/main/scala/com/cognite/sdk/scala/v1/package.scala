// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala

import scala.concurrent.duration._

import cats.Id
import com.softwaremill.sttp.{HttpURLConnectionBackend, SttpBackend}
import com.cognite.sdk.scala.common.{GzipSttpBackend, RetryingBackend}

package object v1 {
  implicit val sttpBackend: SttpBackend[Id, Nothing] =
    new RetryingBackend[Id, Nothing](
      new GzipSttpBackend[Id, Nothing](
        HttpURLConnectionBackend()
      ),
      initialRetryDelay = 100.millis,
      maxRetryDelay = 200.millis
    )
}
