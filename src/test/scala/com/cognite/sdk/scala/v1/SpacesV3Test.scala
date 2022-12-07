// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1

import cats.effect.unsafe.implicits.global
import org.scalatest.BeforeAndAfterAll

@SuppressWarnings(
  Array(
    "org.wartremover.warts.PublicInference",
    "org.wartremover.warts.NonUnitStatements"
  )
)
class SpacesV3Test extends CommonDataModelTestHelper with BeforeAndAfterAll{
  private val spaceNamePrefix = "test-space-scala-sdk-spacetest"

  override def beforeAll(): Unit = {
    val spacesToCreate = Seq(
      SpaceCreateDefinition(space = s"$spaceNamePrefix-1"),
      SpaceCreateDefinition(space = s"$spaceNamePrefix-2"),
      SpaceCreateDefinition(space = s"$spaceNamePrefix-3"),
      SpaceCreateDefinition(space = s"$spaceNamePrefix-4")
    )
    blueFieldClient.spacesv3.createItems(spacesToCreate).unsafeRunSync()
    ()
  }
    ignore should "create spaces" in {
    val spacesToCreate = Seq(
      SpaceCreateDefinition(space = s"$spaceNamePrefix-11"),
      SpaceCreateDefinition(space = s"$spaceNamePrefix-22"),
      SpaceCreateDefinition(space = s"$spaceNamePrefix-33"),
      SpaceCreateDefinition(space = s"$spaceNamePrefix-44")
    )

    val spaces = blueFieldClient.spacesv3.createItems(spacesToCreate).unsafeRunSync()
    spaces.size shouldBe 4
    // TODO Not implemented in the FDM API yet.
    // val spaceByIds = spacesToCreate.map(SpaceById(_))
    // blueFieldClient.spacesv3.deleteItems(spaceByIds).unsafeRunSync()
    // blueFieldClient.spacesv3.retrieveItems(spaceByIds).unsafeRunSync().size shouldBe 0
  }

  ignore should "retrieve spaces" in {
    val spaces = blueFieldClient.spacesv3.retrieveItems(Seq(
          SpaceById(space = s"$spaceNamePrefix-1"),
         SpaceById(space = s"$spaceNamePrefix-2"),
        SpaceById(space = s"$spaceNamePrefix-3"),
        SpaceById(space = s"$spaceNamePrefix-4"))).unsafeRunSync()
    spaces.size shouldBe 4
  }

  ignore should "list spaces" in {
    val spacePrefix = "scala-sdk-test-list-stream"
    val manySpaces = (1 to 200).map(id => SpaceCreateDefinition(s"$spacePrefix-${id.toString}"))
    val createdSpaces = blueFieldClient.spacesv3.createItems(manySpaces).unsafeRunSync()
    createdSpaces.size shouldBe 200

    val spaces = blueFieldClient.spacesv3.listWithCursor(cursor = None, limit = Some(1), includeGlobal = None).unsafeRunSync()
    spaces.items.size shouldBe 1
    val allSpaces = blueFieldClient.spacesv3.listStream(limit = None, includeGlobal = Some(true)).compile.toVector.unsafeRunSync()
    assert(allSpaces.size >= 200)
  }
}
