package com.cognite.sdk.scala.v1

import com.cognite.sdk.scala.common.{ReadBehaviours, SdkTest}

class ApiKeysTest extends SdkTest with ReadBehaviours {
  "ApiKeys" should behave like readable(client.apiKeys, supportsLimit = false)
}
