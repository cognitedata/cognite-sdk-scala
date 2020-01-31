package com.cognite.sdk.scala.v1

import com.cognite.sdk.scala.common.{ReadBehaviours, SdkTestSpec}

class GroupsTest extends SdkTestSpec with ReadBehaviours {
  "Groups" should behave like readable(client.groups, supportsLimit = false)
}
