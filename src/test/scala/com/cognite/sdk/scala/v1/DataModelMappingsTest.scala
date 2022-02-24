// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1

import com.cognite.sdk.scala.common.RetryWhile

import scala.collection.immutable.Seq

@SuppressWarnings(
  Array(
    "org.wartremover.warts.OptionPartial",
    "org.wartremover.warts.PublicInference"
  )
)
class DataModelMappingsTest extends CommonDataModelTestHelper with RetryWhile {

  "DataModelMappings" should "list all data models mappings" in {
    val dataModelMappings = blueFieldClient.dataModelMappings.list().unsafeRunSync().toList
    dataModelMappings.nonEmpty shouldBe true
  }

  it should "get data models mappings by externalIds" in {
    val dataModelMappings =
      blueFieldClient.dataModelMappings
        .retrieveByExternalIds(
          Seq("Equipment-996bb3e4", "Equipment-9fdb2ad4")
        )
        .unsafeRunSync()
        .toList
    dataModelMappings.size shouldBe 2
  }

}
