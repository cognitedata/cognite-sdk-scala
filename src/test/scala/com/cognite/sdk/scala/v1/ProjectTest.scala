// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1

import com.cognite.sdk.scala.common.SdkTestSpec

@SuppressWarnings(Array("org.wartremover.warts.NonUnitStatements"))
class ProjectTest extends SdkTestSpec {
  // TODO: We don't have sufficient permissions in publicdata or Greenfield
  "Project" should "be retrievable and correct" ignore {
    client.project.unsafeRunSync().name shouldNot be (empty)
    client.project.unsafeRunSync().urlName shouldNot be (empty)
  }
}
