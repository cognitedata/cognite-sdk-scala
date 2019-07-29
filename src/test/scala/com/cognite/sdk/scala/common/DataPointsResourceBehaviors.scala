package com.cognite.sdk.scala.common

import java.time.Instant

import com.softwaremill.sttp.Id
import org.scalatest.{FlatSpec, Matchers}

trait DataPointsResourceBehaviors[I] extends Matchers { this: FlatSpec =>
  private val startTime = System.currentTimeMillis()
  private val endTime = startTime + 20*1000
  private val testDataPoints = (startTime to endTime by 1000).map(t =>
    DataPoint(Instant.ofEpochMilli(t), math.random))

  def withTimeSeriesId(testCode: I => Any): Unit

  def dataPointsResource(dataPoints: DataPointsResource[Id, I]): Unit = {
    it should "be possible to insert and delete numerical data points" in withTimeSeriesId { timeSeriesId =>
      dataPoints.insertById(timeSeriesId, testDataPoints)

      Thread.sleep(3000)
      val points = dataPoints.queryById(timeSeriesId, startTime, endTime + 1)
      points should have size testDataPoints.size.toLong

      val latest = dataPoints.getLatestDataPointById(timeSeriesId)
      latest.isDefined should be (true)
      val latestPoint = latest.get
      testDataPoints.toList should contain (latestPoint)

      dataPoints.deleteRangeById(timeSeriesId, startTime, endTime + 1)
      Thread.sleep(10000)
      val pointsAfterDelete = dataPoints.queryById(timeSeriesId, startTime, endTime + 1)
      pointsAfterDelete should have size 0
    }
  }
}
