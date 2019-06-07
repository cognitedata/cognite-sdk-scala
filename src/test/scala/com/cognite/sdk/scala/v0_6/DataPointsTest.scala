package com.cognite.sdk.scala.v0_6

import java.util.UUID

import com.cognite.sdk.scala.common.{DataPoint, SdkTest, StringDataPoint}

class DataPointsTest extends SdkTest {
  private val client = new Client()
  private val startTime = System.currentTimeMillis()
  private val endTime = startTime + 20*1000
  private val testDataPoints = (startTime to endTime by 1000).map(DataPoint(_, math.random().doubleValue()))
  private val testStringDataPoints = (startTime to endTime by 1000).map(StringDataPoint(_, math.random().doubleValue().toString))

  private def withTimeSeries(testCode: TimeSeries => Any) = {
    val timeSeries = client.timeSeries.create(Seq(TimeSeries(name = s"data-points-test-${UUID.randomUUID().toString}"))).unsafeBody.head
    try {
      testCode(timeSeries)
    } finally {
      client.timeSeries.deleteByIds(Seq(timeSeries.id)).unsafeBody
    }
  }

  private def withStringTimeSeries(testCode: TimeSeries => Any) = {
    val timeSeries = client.timeSeries.create(Seq(TimeSeries(name = s"string-data-points-test-${UUID.randomUUID().toString}", isString = true))).unsafeBody.head
    try {
      testCode(timeSeries)
    } finally {
      client.timeSeries.deleteByIds(Seq(timeSeries.id)).unsafeBody
    }
  }

  //  implicit val errorOrUnitDecoder: Decoder[Either[CdpApiError[CogniteId], Unit]] =
//    EitherDecoder.eitherDecoder[CdpApiError[CogniteId], Unit]
  it should "be possible to insert numerical data points" in withTimeSeries { timeSeries =>
    client.dataPoints.insertById(timeSeries.id, testDataPoints).isSuccess should be (true)
    Thread.sleep(3000)
    val points = client.dataPoints.queryById(timeSeries.id, startTime, endTime + 1).unsafeBody
    points should have size testDataPoints.size.toLong

    val latest = client.dataPoints.getLatestDataPointById(timeSeries.id).unsafeBody
    latest.isDefined should be (true)
    val latestPoint = latest.get
    testDataPoints should contain (latestPoint)

    client.dataPoints.deleteRangeById(timeSeries.id, startTime, endTime + 1)
    Thread.sleep(15000)
    val pointsAfterDelete = client.dataPoints.queryById(timeSeries.id, startTime, endTime + 1).unsafeBody
    pointsAfterDelete should have size 0
  }

  it should "be possible to insert string data points" in withStringTimeSeries { stringTimeSeries =>
    client.dataPoints.insertStringsById(stringTimeSeries.id, testStringDataPoints).isSuccess should be (true)

    Thread.sleep(3000)
    val points = client.dataPoints.queryStringsById(stringTimeSeries.id, startTime, endTime + 1).unsafeBody
    points should have size testDataPoints.size.toLong

    val latest = client.dataPoints.getLatestStringDataPointById(stringTimeSeries.id).unsafeBody
    latest.isDefined should be (true)
    val latestPoint = latest.get
    testStringDataPoints should contain (latestPoint)

    client.dataPoints.deleteRangeById(stringTimeSeries.id, startTime, endTime + 1)
    Thread.sleep(15000)
    val pointsAfterDelete = client.dataPoints.queryById(stringTimeSeries.id, startTime, endTime + 1).unsafeBody
    pointsAfterDelete should have size 0
  }
}
