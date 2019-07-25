package com.cognite.sdk.scala

import cats.Id
import com.softwaremill.sttp.{HttpURLConnectionBackend, SttpBackend}

package object v1 {
  implicit val sttpBackend: SttpBackend[Id, Nothing] = HttpURLConnectionBackend()
}
