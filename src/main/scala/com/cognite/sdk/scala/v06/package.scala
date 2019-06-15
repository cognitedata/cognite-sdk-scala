package com.cognite.sdk.scala

import com.softwaremill.sttp.{HttpURLConnectionBackend, Id, SttpBackend}

package object v06 {
  implicit val sttpBackend: SttpBackend[Id, Nothing] = HttpURLConnectionBackend()
}
