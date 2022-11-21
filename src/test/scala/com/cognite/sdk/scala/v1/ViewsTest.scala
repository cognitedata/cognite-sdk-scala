// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1

import cats.effect.unsafe.implicits.global
import com.cognite.sdk.scala.common.{RetryWhile}
import java.util.UUID

@SuppressWarnings(
  Array(
    "org.wartremover.warts.PublicInference",
    "org.wartremover.warts.NonUnitStatements"
  )
)
class ViewsTest extends CommonDataModelTestHelper with RetryWhile {
  it should "create views" in {
    val uuid = UUID.randomUUID.toString // TODO no need to use uuid for externalId when API is in place

    val created = localClient.views.createItems(Seq(
      ViewCreateDefinition(
         space = "test",
         externalId = uuid,
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
