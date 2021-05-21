// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1

import java.util.UUID
import com.cognite.sdk.scala.common.{SdkTestSpec, StringDataPointsResourceBehaviors}

@SuppressWarnings(Array("org.wartremover.warts.TraversableOps"))
class StringDataPointsTest extends SdkTestSpec with StringDataPointsResourceBehaviors {
  override def withStringTimeSeries(testCode: TimeSeries => Any): Unit = {
    val name = Some(s"string-data-points-test-${UUID.randomUUID().toString}")
    val timeSeries = client.timeSeries.createFromRead(
      Seq(TimeSeries(name = name, externalId = name, isString = true))
    ).head
    try {
      val _ = testCode(timeSeries)
    } catch {
      case t: Throwable => throw t
    } finally {
      client.timeSeries.deleteByIds(Seq(timeSeries.id))
    }
  }

  it should behave like stringDataPointsResource(client.dataPoints)
}
