package com.cognite.sdk.scala.common

import com.softwaremill.sttp.{HttpURLConnectionBackend, Id, SttpBackend}
import org.scalatest.{FlatSpec, Matchers}

abstract class SdkTest extends FlatSpec with Matchers {
  private val apiKey = System.getenv("COGNITE_API_KEY")
  implicit val backend: SttpBackend[Id, Nothing] = HttpURLConnectionBackend()
  implicit val auth: Auth = ApiKeyAuth(apiKey)
}
