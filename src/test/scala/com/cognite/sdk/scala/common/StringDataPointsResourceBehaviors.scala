package com.cognite.sdk.scala.common

import java.time.Instant

import com.cognite.sdk.scala.v1.TimeSeries
import com.softwaremill.sttp.Id
import org.scalatest.{FlatSpec, Matchers}

trait StringDataPointsResourceBehaviors extends Matchers { this: FlatSpec =>
  private val startTime = System.currentTimeMillis()
  private val endTime = startTime + 20*1000
  private val testStringDataPoints = (startTime to endTime by 1000).map(t =>
    StringDataPoint(Instant.ofEpochMilli(t), math.random.toString))

  def withStringTimeSeries(testCode: TimeSeries => Any): Unit

  def stringDataPointsResource(dataPoints: DataPointsResource[Id]): Unit =
    it should "be possible to insert and delete string data points" in withStringTimeSeries {
      stringTimeSeries =>
      val stringTimeSeriesId = stringTimeSeries.id
        val stringTimeSeriesExternalId = stringTimeSeries.externalId.get
        dataPoints.insertStringsById(stringTimeSeriesId, testStringDataPoints)

        Thread.sleep(15000)
        val points = dataPoints.queryStringsById(stringTimeSeriesId, startTime, endTime + 1)
        points should have size testStringDataPoints.size.toLong

        val latest = dataPoints.getLatestStringDataPointById(stringTimeSeriesId)
        latest.isDefined should be(true)
        val latestPoint = latest.get
        testStringDataPoints.toList should contain(latestPoint)

        dataPoints.deleteRangeById(stringTimeSeriesId, startTime, endTime + 1)
        Thread.sleep(15000)
        val pointsAfterDelete = dataPoints.queryById(stringTimeSeriesId, startTime, endTime + 1)
        pointsAfterDelete should have size 0

        dataPoints.insertStringsByExternalId(stringTimeSeriesExternalId, testStringDataPoints)

        Thread.sleep(15000)
        val points2 = dataPoints.queryStringsByExternalId(stringTimeSeriesExternalId, startTime, endTime + 1)
        points2 should have size testStringDataPoints.size.toLong

        val latest2 = dataPoints.getLatestStringDataPointByExternalId(stringTimeSeriesExternalId)
        latest2.isDefined should be(true)
        val latestPoint2 = latest2.get
        testStringDataPoints.toList should contain(latestPoint2)

        dataPoints.deleteRangeByExternalId(stringTimeSeriesExternalId, startTime, endTime + 1)
        Thread.sleep(15000)
        val pointsAfterDelete2 = dataPoints.queryByExternalId(stringTimeSeriesExternalId, startTime, endTime + 1)
        pointsAfterDelete2 should have size 0
    }
}
