package com.cognite.sdk.scala.v1

import java.util.UUID
import com.cognite.sdk.scala.common.{SdkTest, StringDataPointsResourceBehaviors}

class StringDataPointsTest extends SdkTest with StringDataPointsResourceBehaviors {
  override def withStringTimeSeries(testCode: TimeSeries => Any): Unit = {
    val name = s"string-data-points-test-${UUID.randomUUID().toString}"
    val timeSeries = client.timeSeries.createFromRead(
      Seq(TimeSeries(name = name, externalId = Some(name), isString = true))
    ).head
    try {
      val _ = testCode(timeSeries)
    } finally {
      client.timeSeries.deleteByIds(Seq(timeSeries.id))
    }
  }

  it should behave like stringDataPointsResource(client.dataPoints)
}
