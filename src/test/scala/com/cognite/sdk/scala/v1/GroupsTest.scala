package com.cognite.sdk.scala.v1

import com.cognite.sdk.scala.common.{ReadBehaviours, SdkTest}

class GroupsTest extends SdkTest with ReadBehaviours {
  "Groups" should behave like readable(client.groups)
}
