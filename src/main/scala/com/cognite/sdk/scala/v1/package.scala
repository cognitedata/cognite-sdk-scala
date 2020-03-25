package com.cognite.sdk.scala

import java.util.concurrent.Executors

import scala.concurrent.duration._

import cats.Id
import com.softwaremill.sttp.{HttpURLConnectionBackend, SttpBackend}
import com.cognite.sdk.scala.common.{GzipSttpBackend, RetryingBackend}

import scala.concurrent.ExecutionContext

package object v1 {
  implicit val executionContext: ExecutionContext =
    ExecutionContext.fromExecutor(Executors.newCachedThreadPool())
  implicit val sttpBackend: SttpBackend[Id, Nothing] =
    new RetryingBackend[Id, Nothing](
      new GzipSttpBackend[Id, Nothing](
        HttpURLConnectionBackend()
      ),
      initialRetryDelay = 100.millis,
      maxRetryDelay = 200.millis
    )
}
