// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1

import com.cognite.sdk.scala.common.{ReadBehaviours, SdkTestSpec}

class GroupsTest extends SdkTestSpec with ReadBehaviours {
  "Groups" should behave like readable(client.groups, supportsLimit = false)
}
