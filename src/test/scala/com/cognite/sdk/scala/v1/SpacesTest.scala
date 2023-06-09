// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1

import cats.effect.unsafe.implicits.global
import com.cognite.sdk.scala.common.RetryWhile

import java.util.UUID
import scala.collection.immutable.Seq

class SpacesTest extends CommonDataModelTestHelper with RetryWhile {

  private val uniqueSpace = s"sdk-test-space-${UUID.randomUUID().toString.substring(8)}"

  private val randomSpace = SpaceCreate(
    uniqueSpace,
    Some(s"description-$uniqueSpace"),
    Some(s"name-$uniqueSpace")
  )

  // TODO: enable all the tests when fdm team enables delete space, we have limit on the number of spaces created per project
  "DataModels" should "create space" ignore {
    val spaces = blueFieldClient.spaces
      .createItems(Seq(randomSpace))
      .unsafeRunSync()
      .toList
    spaces.map(s => SpaceCreate(s.space, s.description, s.name)) should contain(randomSpace)
  }

  ignore should "retrieve space by ids" in {
    val spaces = blueFieldClient.spaces.retrieveBySpaceIds(Seq(uniqueSpace)).unsafeRunSync().toList
    spaces.map(_.space) should contain(uniqueSpace)
  }

  ignore should "delete space" in {
    blueFieldClient.spaces.deleteItems(Seq(uniqueSpace)).unsafeRunSync()
    val spaces = blueFieldClient.spaces.retrieveBySpaceIds(Seq(uniqueSpace)).unsafeRunSync().toList
    spaces shouldBe empty
  }

}
