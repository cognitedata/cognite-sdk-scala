// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1

import cats.Id
import com.cognite.sdk.scala.common._

class ApiKeysTest extends SdkTestSpec with ReadBehaviours {
  private val apiKey = Option(System.getenv("TEST_API_KEY"))
                  .getOrElse(throw new RuntimeException("TEST_API_KEY not set"))
  override lazy val auth: Auth = ApiKeyAuth(apiKey)

  override lazy val client = GenericClient.forAuth[Id](
    "scala-sdk-test", auth)

  "ApiKeys" should behave like readable(client.apiKeys, supportsLimit = false)
}
