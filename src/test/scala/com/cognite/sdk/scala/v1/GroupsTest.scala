// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1

import com.cognite.sdk.scala.common.{ReadBehaviours, WritableBehaviors, SdkTestSpec}

class GroupsTest extends SdkTestSpec with ReadBehaviours with WritableBehaviors {
  "Groups" should behave like readable(client.groups, supportsLimit = false)
  it should behave like writable(client.groups, Some(client.groups),
    readExamples = Seq(
      Group(
        name = s"GroupsTest-${shortRandom()}",
        sourceId = Some(s"${shortRandom()}"),
        capabilities = Seq(
          Map("eventsAcl" -> Capability(actions = Seq("READ"), scope = Map("all" -> Map())))
        ),
        id = -1L,
        isDeleted = false,
        deletedTime = None
      )
    ),
    createExamples = Seq(
      GroupCreate(
        name = s"GroupsTest-${shortRandom()}",
        sourceId = Some(s"${shortRandom()}"),
        capabilities = Seq(
          Map("assetsAcl" -> Capability(actions = Seq("READ"), scope = Map("all" -> Map())))
        )
      )
    ),
    idsThatDoNotExist = Seq(11111111L),
    supportsMissingAndThrown = false)
}
