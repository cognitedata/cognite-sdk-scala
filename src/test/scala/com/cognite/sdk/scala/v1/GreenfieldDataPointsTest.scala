package com.cognite.sdk.scala.v1

import java.util.UUID
import com.cognite.sdk.scala.common.{DataPointsResourceBehaviors, SdkTest}

class GreenfieldDataPointsTest extends SdkTest with DataPointsResourceBehaviors {
  override def withTimeSeries(testCode: TimeSeries => Any): Unit = {
    val name = s"data-points-test-${UUID.randomUUID().toString}"
    val timeSeries = greenfieldClient.timeSeries.createFromRead(
      Seq(TimeSeries(name = name, externalId = Some(name)))
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
