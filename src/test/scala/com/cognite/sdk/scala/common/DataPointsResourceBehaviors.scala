package com.cognite.sdk.scala.common

import java.time.Instant

import com.cognite.sdk.scala.v1.{DataPointsByExternalIdResponse, DataPointsByIdResponse, TimeSeries}
import com.cognite.sdk.scala.v1.resources.DataPointsResource
import com.softwaremill.sttp.Id
import org.scalatest.{FlatSpec, Matchers}

trait DataPointsResourceBehaviors extends Matchers with RetryWhile { this: FlatSpec =>
  private val startTime = System.currentTimeMillis()
  private val start = Instant.ofEpochMilli(startTime)
  private val endTime = startTime + 20*1000
  private val end = Instant.ofEpochMilli(endTime)
  private val testDataPoints = (startTime to endTime by 1000).map(t =>
    DataPoint(Instant.ofEpochMilli(t), math.random))

  def withTimeSeries(testCode: TimeSeries => Any): Unit

   // scalastyle:off
  def dataPointsResource(dataPoints: DataPointsResource[Id]): Unit =
    it should "be possible to insert and delete numerical data points" in withTimeSeries {
      timeSeries =>
        val timeSeriesId = timeSeries.id
        val timeSeriesExternalId = timeSeries.externalId.get
        dataPoints.insertById(timeSeriesId, testDataPoints)

        retryWithExpectedResult[DataPointsByIdResponse](
          dataPoints.queryById(timeSeriesId, start, end.plusMillis(1)),
          p => p.datapoints should have size testDataPoints.size.toLong
        )

        retryWithExpectedResult[DataPointsByIdResponse](
          dataPoints.queryById(timeSeriesId, start, end.plusMillis(1), Some(3)),
          p => p.datapoints should have size 3
        )

        retryWithExpectedResult[Option[DataPoint]](
          dataPoints.getLatestDataPointById(timeSeriesId),
          dp => {
            dp.isDefined shouldBe true
            testDataPoints.toList should contain(dp.get)
          }
        )

        dataPoints.deleteRangeById(timeSeriesId, start, end.plusMillis(1))
        retryWithExpectedResult[DataPointsByIdResponse](
          dataPoints.queryById(timeSeriesId, start, end.plusMillis(1)),
          dp => dp.datapoints should have size 0
        )

        dataPoints.insertByExternalId(timeSeriesExternalId, testDataPoints)
        retryWithExpectedResult[DataPointsByExternalIdResponse](
          dataPoints.queryByExternalId(timeSeriesExternalId, start, end.plusMillis(1)),
          p2 => p2.datapoints should have size testDataPoints.size.toLong
        )

        retryWithExpectedResult[DataPointsByExternalIdResponse](
          dataPoints.queryByExternalId(timeSeriesExternalId, start, end.plusMillis(1), Some(5)),
          p2 => p2.datapoints should have size 5
        )

        retryWithExpectedResult[Option[DataPoint]](
          dataPoints.getLatestDataPointByExternalId(timeSeriesExternalId),
          { l2 =>
            l2.isDefined shouldBe true
            testDataPoints.toList should contain(l2.get)
          }
        )

        dataPoints.deleteRangeByExternalId(timeSeriesExternalId, start, end.plusMillis(1))
        retryWithExpectedResult[DataPointsByExternalIdResponse](
          dataPoints.queryByExternalId(timeSeriesExternalId, start, end.plusMillis(1)),
          pad => pad.datapoints should have size 0
        )
    }
}
