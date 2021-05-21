// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1

import java.util.UUID
import com.cognite.sdk.scala.common.{DataPointsResourceBehaviors, SdkTestSpec}

@SuppressWarnings(Array("org.wartremover.warts.TraversableOps"))
class GreenfieldDataPointsTest extends SdkTestSpec with DataPointsResourceBehaviors {
  override def withTimeSeries(testCode: TimeSeries => Any): Unit = {
    val name = Some(s"data-points-test-${UUID.randomUUID().toString}")
    val timeSeries = greenfieldClient.timeSeries.createFromRead(
      Seq(TimeSeries(name = name, externalId = name))
    ).head
    try {
      val _ = testCode(timeSeries)
    } catch {
      case t: Throwable => throw t
    } finally {
      greenfieldClient.timeSeries.deleteByIds(Seq(timeSeries.id))
    }
  }

  it should behave like dataPointsResource(greenfieldClient.dataPoints)
}
