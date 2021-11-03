// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.common

import cats.Id

import java.time.Instant
import com.cognite.sdk.scala.v1.{CogniteExternalId, CogniteInternalId, DataPointsByExternalIdResponse, DataPointsByIdResponse, TimeSeries}
import com.cognite.sdk.scala.v1.resources.DataPointsResource
import org.scalatest.OptionValues
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

@SuppressWarnings(Array("org.wartremover.warts.NonUnitStatements"))
trait DataPointsResourceBehaviors extends Matchers with OptionValues with RetryWhile { this: AnyFlatSpec =>
  private val startTime = System.currentTimeMillis()
  private val start = Instant.ofEpochMilli(startTime)
  private val endTime = startTime + 20*1000
  private val end = Instant.ofEpochMilli(endTime)
  private val testDataPoints = (startTime to endTime by 1000).map(t =>
    DataPoint(Instant.ofEpochMilli(t), java.lang.Math.random()))

  def withTimeSeries(testCode: TimeSeries => Any): Unit

   // scalastyle:off
  def dataPointsResource(dataPoints: DataPointsResource[Id]): Unit = {
    it should "be possible to insert and delete numerical data points" in withTimeSeries {
      timeSeries =>
        val timeSeriesId = timeSeries.id
        val timeSeriesExternalId = timeSeries.externalId.value
        dataPoints.insert(CogniteInternalId(timeSeriesId), testDataPoints)

        retryWithExpectedResult[DataPointsByIdResponse](
          dataPoints.queryById(timeSeriesId, start, end.plusMillis(1)),
          p => p.datapoints should have size testDataPoints.size.toLong
        )

        retryWithExpectedResult[DataPointsByIdResponse](
          dataPoints.queryById(timeSeriesId, start, end.plusMillis(1), Some(3)),
          p => p.datapoints should have size 3
        )

        retryWithExpectedResult[Option[DataPoint]](
          dataPoints.getLatestDataPoint(CogniteInternalId(timeSeriesId)),
          dp => {
            dp.isDefined shouldBe true
            testDataPoints.toList should contain(dp.value)
          }
        )

        val latestStartDp = dataPoints.getLatestDataPoint(
          CogniteInternalId(timeSeriesId),
          start.plusMillis(1).toEpochMilli.toString
        )
        latestStartDp.isDefined shouldBe true
        testDataPoints.headOption.map(_.value) shouldBe latestStartDp.map(_.value)

        val latestEndDp = dataPoints.getLatestDataPoint(
          CogniteInternalId(timeSeriesId),
          end.plusMillis(1).toEpochMilli.toString
        )
        latestEndDp.isDefined shouldBe true
        testDataPoints.lastOption.map(_.value) shouldBe latestEndDp.map(_.value)

        dataPoints.deleteRangeById(timeSeriesId, start, end.plusMillis(1))
        retryWithExpectedResult[DataPointsByIdResponse](
          dataPoints.queryById(timeSeriesId, start, end.plusMillis(1)),
          dp => dp.datapoints should have size 0
        )

        dataPoints.insert(CogniteExternalId(timeSeriesExternalId), testDataPoints)
        retryWithExpectedResult[DataPointsByExternalIdResponse](
          dataPoints.queryByExternalId(timeSeriesExternalId, start, end.plusMillis(1)),
          p2 => p2.datapoints should have size testDataPoints.size.toLong
        )

        retryWithExpectedResult[DataPointsByExternalIdResponse](
          dataPoints.queryByExternalId(timeSeriesExternalId, start, end.plusMillis(1), Some(5)),
          p2 => p2.datapoints should have size 5
        )

        retryWithExpectedResult[Option[DataPoint]](
          dataPoints.getLatestDataPoint(CogniteExternalId(timeSeriesExternalId)),
          { l2 =>
            l2.isDefined shouldBe true
            testDataPoints.toList should contain(l2.value)
          }
        )

        val latestStartDataPoint = dataPoints.getLatestDataPoint(
          CogniteExternalId(timeSeriesExternalId),
          start.plusMillis(1).toEpochMilli.toString
        )
        latestStartDataPoint.isDefined shouldBe true
        testDataPoints.headOption.map(_.value) shouldBe latestStartDataPoint.map(_.value)

        val latestEndDataPoint = dataPoints.getLatestDataPoint(
          CogniteExternalId(timeSeriesExternalId),
          end.plusMillis(1).toEpochMilli.toString
        )
        latestEndDataPoint.isDefined shouldBe true
        testDataPoints.lastOption.map(_.value) shouldBe latestEndDataPoint.map(_.value)

        dataPoints.deleteRangeByExternalId(timeSeriesExternalId, start, end.plusMillis(1))
        retryWithExpectedResult[DataPointsByExternalIdResponse](
          dataPoints.queryByExternalId(timeSeriesExternalId, start, end.plusMillis(1)),
          pad => pad.datapoints should have size 0,
          retriesRemaining = 20
        )
    }

    it should "be possible to insertById and insertByExternalId and delete numerical data points" in withTimeSeries {
      timeSeries =>
        val timeSeriesId = timeSeries.id
        val timeSeriesExternalId = timeSeries.externalId.value
        dataPoints.insertById(timeSeriesId, testDataPoints)

        retryWithExpectedResult[DataPointsByIdResponse](
          dataPoints.queryById(timeSeriesId, start, end.plusMillis(1)),
          p => p.datapoints should have size testDataPoints.size.toLong
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

        dataPoints.deleteRangeByExternalId(timeSeriesExternalId, start, end.plusMillis(1))
        retryWithExpectedResult[DataPointsByExternalIdResponse](
          dataPoints.queryByExternalId(timeSeriesExternalId, start, end.plusMillis(1)),
          pad => pad.datapoints should have size 0
        )
    }

    it should "be an error to insert numerical data points for non-existing time series" in {
      val unknownId = 991919L
      val thrown = the[CdpApiException] thrownBy dataPoints.insert(CogniteInternalId(unknownId), testDataPoints)

      val itemsNotFound = thrown.missing.value
      val notFoundIds =
        itemsNotFound.map(jsonObj => jsonObj("id").value.asNumber.value.toLong.value)
      itemsNotFound should have size 1
      assert(notFoundIds.headOption.value === unknownId)
    }

    it should "be an error to delete numerical data points for non-existing time series" in {
      val unknownId = 991999L
      val thrown = the[CdpApiException] thrownBy dataPoints.deleteRangeById(unknownId, start, end)
      val itemsNotFound = thrown.missing.value
      val notFoundIds =
        itemsNotFound.map(jsonObj => jsonObj("id").value.asNumber.value.toLong.value)
      itemsNotFound should have size 1
      assert(notFoundIds.headOption.value === unknownId)
    }
  }
}
