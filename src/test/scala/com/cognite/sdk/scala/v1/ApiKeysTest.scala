// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1

import com.cognite.sdk.scala.common.{ReadBehaviours, SdkTestSpec}

class ApiKeysTest extends SdkTestSpec with ReadBehaviours {
  "ApiKeys" should behave like readable(client.apiKeys, supportsLimit = false)
}
