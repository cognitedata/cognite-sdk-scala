package com.cognite.sdk.scala.v1

import com.cognite.sdk.scala.common.SdkTest

class ProjectTest extends SdkTest {
  // TODO: We don't have sufficient permissions in publicdata or Greenfield
  "Project" should "be retrievable and correct" ignore {
    greenfieldClient.project.name shouldNot be (empty)
    greenfieldClient.project.urlName shouldNot be (empty)
  }
}
