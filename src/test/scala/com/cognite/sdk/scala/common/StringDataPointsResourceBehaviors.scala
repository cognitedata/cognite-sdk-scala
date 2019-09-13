package com.cognite.sdk.scala.common

import java.time.Instant

import com.softwaremill.sttp.Id
import org.scalatest.{FlatSpec, Matchers}

trait StringDataPointsResourceBehaviors[I] extends Matchers { this: FlatSpec =>
  private val startTime = System.currentTimeMillis()
  private val endTime = startTime + 20 * 1000
  private val testStringDataPoints = (startTime to endTime by 1000)
    .map(t => StringDataPoint(Instant.ofEpochMilli(t), math.random.toString))

  def withStringTimeSeriesId(testCode: I => Any): Unit

  def stringDataPointsResource(dataPoints: DataPointsResource[Id, I]): Unit =
    it should "be possible to insert and delete string data points" in withStringTimeSeriesId {
      stringTimeSeriesId =>
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
    }
}
