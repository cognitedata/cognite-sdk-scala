package com.cognite.sdk.scala.common

import java.time.Instant

import com.cognite.sdk.scala.v1.TimeSeries
import com.cognite.sdk.scala.v1.resources.DataPointsResource
import com.softwaremill.sttp.Id
import org.scalatest.{FlatSpec, Matchers}

trait DataPointsResourceBehaviors extends Matchers { this: FlatSpec =>
  private val startTime = System.currentTimeMillis()
  private val endTime = startTime + 20*1000
  private val testDataPoints = (startTime to endTime by 1000).map(t =>
    DataPoint(Instant.ofEpochMilli(t), math.random))

  def withTimeSeries(testCode: TimeSeries => Any): Unit

  def dataPointsResource(dataPoints: DataPointsResource[Id]): Unit =
    it should "be possible to insert and delete numerical data points" in withTimeSeries {
      timeSeries =>
        val timeSeriesId = timeSeries.id
        val timeSeriesExternalId = timeSeries.externalId.get
        dataPoints.insertById(timeSeriesId, testDataPoints)

        Thread.sleep(15000)
        val points = dataPoints.queryById(timeSeriesId, startTime, endTime + 1)
        points should have size testDataPoints.size.toLong

        val pointsWithLimit = dataPoints.queryById(timeSeriesId, startTime, endTime + 1, Some(3))
        pointsWithLimit should have size 3

        val latest = dataPoints.getLatestDataPointById(timeSeriesId)
        latest.isDefined should be(true)
        val latestPoint = latest.get
        testDataPoints.toList should contain(latestPoint)

        dataPoints.deleteRangeById(timeSeriesId, startTime, endTime + 1)
        Thread.sleep(15000)
        val pointsAfterDelete = dataPoints.queryById(timeSeriesId, startTime, endTime + 1)
        pointsAfterDelete should have size 0

        dataPoints.insertByExternalId(timeSeriesExternalId, testDataPoints)

        Thread.sleep(15000)
        val points2 = dataPoints.queryByExternalId(timeSeriesExternalId, startTime, endTime + 1)
        points2 should have size testDataPoints.size.toLong

        val points2WithLimit = dataPoints.queryByExternalId(timeSeriesExternalId, startTime, endTime + 1, Some(5))
        points2WithLimit should have size 5

        val latest2 = dataPoints.getLatestDataPointByExternalId(timeSeriesExternalId)
        latest2.isDefined should be(true)
        val latestPoint2 = latest.get
        testDataPoints.toList should contain(latestPoint2)

        dataPoints.deleteRangeByExternalId(timeSeriesExternalId, startTime, endTime + 1)
        Thread.sleep(15000)
        val pointsAfterDelete2 =
          dataPoints.queryByExternalId(timeSeriesExternalId, startTime, endTime + 1)
        pointsAfterDelete2 should have size 0
    }
}
