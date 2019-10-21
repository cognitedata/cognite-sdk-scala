package com.cognite.sdk.scala.common

import java.time.Instant

import com.cognite.sdk.scala.v1.{DataPointsByIdResponse, TimeSeries}
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

        retryWithExpectedResult[Seq[DataPointsByIdResponse]](
          dataPoints.queryById(timeSeriesId, start, end.plusMillis(1)),
          None,
          Seq(p => p.head.datapoints should have size testDataPoints.size.toLong)
        )

        retryWithExpectedResult[Seq[DataPointsByIdResponse]](
          dataPoints.queryById(timeSeriesId, start, end.plusMillis(1), Some(3)),
          None,
          Seq(p => p.head.datapoints should have size 3)
        )

        retryWithExpectedResult[Option[DataPoint]](
          dataPoints.getLatestDataPointById(timeSeriesId),
          None,
          Seq(dp => dp.isDefined shouldBe true, dp => testDataPoints.toList should contain(dp.get))
        )

        dataPoints.deleteRangeById(timeSeriesId, start, end.plusMillis(1))
        retryWithExpectedResult[Seq[DataPointsByIdResponse]](
          dataPoints.queryById(timeSeriesId, start, end.plusMillis(1)),
          None,
          Seq(dp => dp.head.datapoints should have size 0)
        )

        dataPoints.insertByExternalId(timeSeriesExternalId, testDataPoints)
        retryWithExpectedResult[Seq[DataPointsByIdResponse]](
          dataPoints.queryByExternalId(timeSeriesExternalId, start, end.plusMillis(1)),
          None,
          Seq(p2 => p2.head.datapoints should have size testDataPoints.size.toLong)
        )

        retryWithExpectedResult[Seq[DataPointsByIdResponse]](
          dataPoints.queryByExternalId(timeSeriesExternalId, start, end.plusMillis(1), Some(5)),
          None,
          Seq(p2 => p2.head.datapoints should have size 5)
        )

        retryWithExpectedResult[Option[DataPoint]](
          dataPoints.getLatestDataPointByExternalId(timeSeriesExternalId),
          None,
          Seq(l2 => l2.isDefined shouldBe true, l2 => testDataPoints.toList should contain(l2.get))
        )

        dataPoints.deleteRangeByExternalId(timeSeriesExternalId, start, end.plusMillis(1))
        retryWithExpectedResult[Seq[DataPointsByIdResponse]](
          dataPoints.queryByExternalId(timeSeriesExternalId, start, end.plusMillis(1)),
          None,
          Seq(pad => pad.head.datapoints should have size 0)
        )
    }
}
