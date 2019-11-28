package com.cognite.sdk.scala.v1

import com.cognite.sdk.scala.common.SdkTest

class ProjectTest extends SdkTest {
  "Project" should "be retrievable and correct" in {
    client.project.name shouldNot be (empty)
    client.project.urlName shouldNot be (empty)
  }
}
