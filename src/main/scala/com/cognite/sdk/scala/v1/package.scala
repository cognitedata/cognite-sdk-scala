package com.cognite.sdk.scala

import cats.Id
import com.softwaremill.sttp.{HttpURLConnectionBackend, SttpBackend}
import com.cognite.sdk.scala.common.RetryingBackend

package object v1 {
  implicit val sttpBackend: SttpBackend[Id, Nothing] =
    new RetryingBackend[Id, Nothing](HttpURLConnectionBackend())
}
