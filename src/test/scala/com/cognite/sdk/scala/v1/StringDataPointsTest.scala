package com.cognite.sdk.scala.v1

import java.util.UUID

import com.cognite.sdk.scala.common.{SdkTest, StringDataPointsResourceBehaviors}

class StringDataPointsTest extends SdkTest with StringDataPointsResourceBehaviors[Long] {
  override def withStringTimeSeriesId(testCode: Long => Any): Unit = {
    val timeSeriesId = client.timeSeries.createFromRead(
      Seq(TimeSeries(name = s"string-data-points-test-${UUID.randomUUID().toString}", isString = true))
    ).head.id
    try {
      val _ = testCode(timeSeriesId)
    } finally {
      client.timeSeries.deleteByIds(Seq(timeSeriesId))
    }
  }

  it should behave like stringDataPointsResource(client.dataPoints)
}
