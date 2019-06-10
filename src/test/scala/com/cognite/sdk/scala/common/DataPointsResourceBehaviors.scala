package com.cognite.sdk.scala.common

import com.softwaremill.sttp.Id
import org.scalatest.{FlatSpec, Matchers}

trait DataPointsResourceBehaviors[I] extends Matchers { this: FlatSpec =>
  private val startTime = System.currentTimeMillis()
  private val endTime = startTime + 20*1000
  private val testDataPoints = (startTime to endTime by 1000).map(DataPoint(_, math.random().doubleValue()))
  private val testStringDataPoints = (startTime to endTime by 1000).map(StringDataPoint(_, math.random().doubleValue().toString))

  def withTimeSeriesId(testCode: I => Any): Unit
  def withStringTimeSeriesId(testCode: I => Any): Unit

  def dataPointsResource(dataPoints: DataPointsResource[Id, I]): Unit = {
    it should "be possible to insert and delete numerical data points" in withTimeSeriesId { timeSeriesId =>
      dataPoints.insertById(timeSeriesId, testDataPoints).isSuccess should be (true)

      Thread.sleep(3000)
      val points = dataPoints.queryById(timeSeriesId, startTime, endTime + 1).unsafeBody
      points should have size testDataPoints.size.toLong

      val latest = dataPoints.getLatestDataPointById(timeSeriesId).unsafeBody
      latest.isDefined should be (true)
      val latestPoint = latest.get
      testDataPoints should contain (latestPoint)

      dataPoints.deleteRangeById(timeSeriesId, startTime, endTime + 1)
      Thread.sleep(15000)
      val pointsAfterDelete = dataPoints.queryById(timeSeriesId, startTime, endTime + 1).unsafeBody
      pointsAfterDelete should have size 0
    }

    it should "be possible to insert and delete string data points" in withStringTimeSeriesId { stringTimeSeriesId =>
      dataPoints.insertStringsById(stringTimeSeriesId, testStringDataPoints).isSuccess should be (true)

      Thread.sleep(3000)
      val points = dataPoints.queryStringsById(stringTimeSeriesId, startTime, endTime + 1).unsafeBody
      points should have size testDataPoints.size.toLong

      val latest = dataPoints.getLatestStringDataPointById(stringTimeSeriesId).unsafeBody
      latest.isDefined should be (true)
      val latestPoint = latest.get
      testStringDataPoints should contain (latestPoint)

      dataPoints.deleteRangeById(stringTimeSeriesId, startTime, endTime + 1)
      Thread.sleep(15000)
      val pointsAfterDelete = dataPoints.queryById(stringTimeSeriesId, startTime, endTime + 1).unsafeBody
      pointsAfterDelete should have size 0
    }
  }
}
