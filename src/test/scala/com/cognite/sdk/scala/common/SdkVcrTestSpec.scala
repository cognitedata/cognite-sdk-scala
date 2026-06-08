// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.common

import cats.effect.IO
import com.cognite.sdk.scala.v1._

/** Extends VcrTestSpec with a persistent `client` (delegates to the per-test
  * `testClient`) and a shared `testDataSet`, analogous to SdkTestSpec.
  *
  * Behavior trait parameters are call-by-name (`=> T`), so `client.events` etc.
  * are not evaluated at class-construction time — only inside test bodies after
  * `beforeEach` has set up the VCR backend for the current test.
  */
@SuppressWarnings(Array("org.wartremover.warts.NonUnitStatements"))
abstract class SdkVcrTestSpec extends VcrTestSpec {

  def client: GenericClient[IO] = testClient

  lazy val testDataSet: DataSet = {
    val list = client.dataSets
      .filter(DataSetFilter(writeProtected = Some(false)))
      .take(1)
      .compile
      .toList
      .unsafeRunSync()
    list.headOption.getOrElse(
      client.dataSets
        .createOne(DataSetCreate(Some("testDataSet"), Some("data set for Scala SDK tests")))
        .unsafeRunSync()
    )
  }
}
