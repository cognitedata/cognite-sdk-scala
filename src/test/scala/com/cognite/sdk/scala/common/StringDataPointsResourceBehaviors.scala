package com.cognite.sdk.scala.common

import java.time.Instant

import com.cognite.sdk.scala.v1.{StringDataPointsByIdResponse, TimeSeries}
import com.cognite.sdk.scala.v1.resources.DataPointsResource
import com.softwaremill.sttp.Id
import org.scalatest.matchers.should.Matchers

trait StringDataPointsResourceBehaviors extends Matchers with RetryWhile { this: FlatSpec =>
  private val startTime = System.currentTimeMillis()
  private val start = Instant.ofEpochMilli(startTime)
  private val endTime = startTime + 20*1000
  private val end = Instant.ofEpochMilli(endTime)
  private val testStringDataPoints = (startTime to endTime by 1000).map(t =>
    StringDataPoint(Instant.ofEpochMilli(t), math.random.toString))

  def withStringTimeSeries(testCode: TimeSeries => Any): Unit

  // scalastyle:off
  def stringDataPointsResource(dataPoints: DataPointsResource[Id]): Unit =
    it should "be possible to insert and delete string data points" in withStringTimeSeries {
      stringTimeSeries =>
      val stringTimeSeriesId = stringTimeSeries.id
        val stringTimeSeriesExternalId = stringTimeSeries.externalId.get
        dataPoints.insertStringsById(stringTimeSeriesId, testStringDataPoints)

        retryWithExpectedResult[Seq[StringDataPointsByIdResponse]](
          dataPoints.queryStringsById(stringTimeSeriesId, start, end.plusMillis(1)),
          p => p.head.datapoints should have size testStringDataPoints.size.toLong
        )

        retryWithExpectedResult[Option[StringDataPoint]](
          dataPoints.getLatestStringDataPointById(stringTimeSeriesId),
          dp => {
            dp.isDefined shouldBe true
            testStringDataPoints.toList should contain(dp.get)
          }
        )

        dataPoints.deleteRangeById(stringTimeSeriesId, start, end.plusMillis(1))
        retryWithExpectedResult[Seq[StringDataPointsByIdResponse]](
          dataPoints.queryStringsById(stringTimeSeriesId, start, end.plusMillis(1)),
          dp => dp.head.datapoints should have size 0
        )

        dataPoints.insertStringsByExternalId(stringTimeSeriesExternalId, testStringDataPoints)
        retryWithExpectedResult[Seq[StringDataPointsByIdResponse]](
          dataPoints.queryStringsByExternalId(stringTimeSeriesExternalId, start, end.plusMillis(1)),
          p2 => p2.head.datapoints should have size testStringDataPoints.size.toLong
        )

        retryWithExpectedResult[Option[StringDataPoint]](
          dataPoints.getLatestStringDataPointByExternalId(stringTimeSeriesExternalId),
          l2 => {
            l2.isDefined shouldBe true
            testStringDataPoints.toList should contain(l2.get)
          }
        )

        dataPoints.deleteRangeById(stringTimeSeriesId, start, end.plusMillis(1))
        retryWithExpectedResult[Seq[StringDataPointsByIdResponse]](
          dataPoints.queryStringsByExternalId(stringTimeSeriesExternalId, start, end.plusMillis(1)),
          pad => pad.head.datapoints should have size 0
        )
    }
}
