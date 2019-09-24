package com.cognite.sdk.scala.v1

import java.util.UUID
import com.cognite.sdk.scala.common.{DataPointsResourceBehaviors, SdkTest}

class GreenfieldDataPointsTest extends SdkTest with DataPointsResourceBehaviors[Long] {
  override def withTimeSeriesId(testCode: Long => Any): Unit = {
    val timeSeriesId = greenfieldClient.timeSeries.createFromRead(
      Seq(TimeSeries(name = s"data-points-test-${UUID.randomUUID().toString}"))
    ).head.id
    try {
      val _ = testCode(timeSeriesId)
    } finally {
      greenfieldClient.timeSeries.deleteByIds(Seq(timeSeriesId))
    }
  }

  it should behave like dataPointsResource(greenfieldClient.dataPoints)
}
