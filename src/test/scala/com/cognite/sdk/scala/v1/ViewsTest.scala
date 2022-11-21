// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1

import cats.effect.unsafe.implicits.global
import com.cognite.sdk.scala.common.{DomainSpecificLanguageFilter, RetryWhile}
import com.cognite.sdk.scala.v1.DataModelType.{EdgeType, NodeType}

import java.util.UUID
import scala.collection.Seq
import scala.collection.immutable.Seq

@SuppressWarnings(
  Array(
    "org.wartremover.warts.PublicInference",
    "org.wartremover.warts.NonUnitStatements"
  )
)
class ViewsTest extends CommonDataModelTestHelper with RetryWhile {
  it should "create views" in {
    val created = localClient.views.createItems(Seq(
      ViewCreateDefinition(
                                           space = "test",
                                           externalId = "test",
                                           name = Some("test"),
                                           description = Some("test"),
                                           filter = None,
                                           implements = None,
                                           version = Some("hehe"),
                                           properties = Map()
                                         )
    )).unsafeRunSync()
    println(s"created = ${created}")
  }
}
