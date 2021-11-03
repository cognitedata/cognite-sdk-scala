// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.common

import cats.Id

import java.time.Instant
import java.util.UUID
import com.cognite.sdk.scala.v1.{CogniteExternalId, CogniteInternalId, StringDataPointsByExternalIdResponse, StringDataPointsByIdResponse, TimeSeries}
import com.cognite.sdk.scala.v1.resources.DataPointsResource
import org.scalatest.OptionValues
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

@SuppressWarnings(Array("org.wartremover.warts.NonUnitStatements"))
trait StringDataPointsResourceBehaviors extends Matchers with OptionValues with RetryWhile { this: AnyFlatSpec =>
  private val startTime = System.currentTimeMillis()
  private val start = Instant.ofEpochMilli(startTime)
  private val endTime = startTime + 20*1000
  private val end = Instant.ofEpochMilli(endTime)
  private val testStringDataPoints = (startTime to endTime by 1000).map(t =>
    StringDataPoint(Instant.ofEpochMilli(t), java.lang.Math.random().toString))

  def withStringTimeSeries(testCode: TimeSeries => Any): Unit

  // scalastyle:off
  def stringDataPointsResource(dataPoints: DataPointsResource[Id]): Unit = {
    it should "be possible to insert and delete string data points" in withStringTimeSeries {
      stringTimeSeries =>
      val stringTimeSeriesId = stringTimeSeries.id
        val stringTimeSeriesExternalId = stringTimeSeries.externalId.value
        dataPoints.insertStrings(CogniteInternalId(stringTimeSeriesId), testStringDataPoints)

        retryWithExpectedResult[StringDataPointsByIdResponse](
          dataPoints.queryStringsById(stringTimeSeriesId, start, end.plusMillis(1)),
          p => p.datapoints should have size testStringDataPoints.size.toLong
        )

        retryWithExpectedResult[Option[StringDataPoint]](
          dataPoints.getLatestStringDataPoint(CogniteInternalId(stringTimeSeriesId)),
          dp => {
            dp.isDefined shouldBe true
            testStringDataPoints.toList should contain(dp.value)
          }
        )

        dataPoints.deleteRangeById(stringTimeSeriesId, start, end.plusMillis(1))
        val resultId = retryWithExpectedResult[StringDataPointsByIdResponse](
          dataPoints.queryStringsById(stringTimeSeriesId, start, end.plusMillis(1)),
          dp => dp.datapoints should have size 0
        )

        resultId.externalId shouldBe Some(stringTimeSeriesExternalId)
        resultId.unit shouldBe stringTimeSeries.unit
        resultId.isString shouldBe true
        resultId.id shouldBe stringTimeSeries.id

        val resultId2: Seq[_] = dataPoints.queryStrings(Seq(CogniteInternalId(stringTimeSeriesId)), start, end.plusMillis(1))
        resultId2.length shouldBe 1
        resultId2.headOption.value shouldBe resultId

        dataPoints.insertStrings(CogniteExternalId(stringTimeSeriesExternalId), testStringDataPoints)
        val resultExternalId: StringDataPointsByExternalIdResponse = retryWithExpectedResult[StringDataPointsByExternalIdResponse](
          dataPoints.queryStringsByExternalId(stringTimeSeriesExternalId, start, end.plusMillis(1)),
          p2 => p2.datapoints should have size testStringDataPoints.size.toLong
        )

        resultExternalId.externalId shouldBe stringTimeSeriesExternalId
        resultExternalId.unit shouldBe stringTimeSeries.unit
        resultExternalId.isString shouldBe true
        resultExternalId.id shouldBe stringTimeSeries.id

        val resultExternalId2: Seq[StringDataPointsByExternalIdResponse] = dataPoints.queryStringsByExternalIds(Seq(stringTimeSeriesExternalId), start, end.plusMillis(1))
        resultExternalId2.length shouldBe 1
        resultExternalId2.headOption.value shouldBe resultExternalId

        retryWithExpectedResult[Option[StringDataPoint]](
          dataPoints.getLatestStringDataPoint(CogniteExternalId(stringTimeSeriesExternalId)),
          l2 => {
            l2.isDefined shouldBe true
            testStringDataPoints.toList should contain(l2.value)
          }
        )

        dataPoints.deleteRangeById(stringTimeSeriesId, start, end.plusMillis(1))
        retryWithExpectedResult[StringDataPointsByExternalIdResponse](
          dataPoints.queryStringsByExternalId(stringTimeSeriesExternalId, start, end.plusMillis(1)),
          pad => pad.datapoints should have size 0
        )
    }

    it should "be possible to insertStringsById and insertStringsByExternalId and delete string data points" in withStringTimeSeries {
      stringTimeSeries =>
        val stringTimeSeriesId = stringTimeSeries.id
        val stringTimeSeriesExternalId = stringTimeSeries.externalId.value
        dataPoints.insertStringsById(stringTimeSeriesId, testStringDataPoints)

        retryWithExpectedResult[StringDataPointsByIdResponse](
          dataPoints.queryStringsById(stringTimeSeriesId, start, end.plusMillis(1)),
          p => p.datapoints should have size testStringDataPoints.size.toLong
        )

        retryWithExpectedResult[Option[StringDataPoint]](
          dataPoints.getLatestStringDataPoint(CogniteInternalId(stringTimeSeriesId)),
          dp => {
            dp.isDefined shouldBe true
            testStringDataPoints.toList should contain(dp.value)
          }
        )

        dataPoints.deleteRangeById(stringTimeSeriesId, start, end.plusMillis(1))
        val resultId = retryWithExpectedResult[StringDataPointsByIdResponse](
          dataPoints.queryStringsById(stringTimeSeriesId, start, end.plusMillis(1)),
          dp => dp.datapoints should have size 0
        )

        resultId.externalId shouldBe Some(stringTimeSeriesExternalId)
        resultId.unit shouldBe stringTimeSeries.unit
        resultId.isString shouldBe true
        resultId.id shouldBe stringTimeSeries.id

        val resultId2: Seq[_] = dataPoints.queryStrings(Seq(CogniteInternalId(stringTimeSeriesId)), start, end.plusMillis(1))
        resultId2.length shouldBe 1
        resultId2.headOption.value shouldBe resultId

        dataPoints.insertStringsByExternalId(stringTimeSeriesExternalId, testStringDataPoints)
        val resultExternalId: StringDataPointsByExternalIdResponse = retryWithExpectedResult[StringDataPointsByExternalIdResponse](
          dataPoints.queryStringsByExternalId(stringTimeSeriesExternalId, start, end.plusMillis(1)),
          p2 => p2.datapoints should have size testStringDataPoints.size.toLong
        )

        resultExternalId.externalId shouldBe stringTimeSeriesExternalId
        resultExternalId.unit shouldBe stringTimeSeries.unit
        resultExternalId.isString shouldBe true
        resultExternalId.id shouldBe stringTimeSeries.id

        val resultExternalId2: Seq[StringDataPointsByExternalIdResponse] = dataPoints.queryStringsByExternalIds(Seq(stringTimeSeriesExternalId), start, end.plusMillis(1))
        resultExternalId2.length shouldBe 1
        resultExternalId2.headOption.value shouldBe resultExternalId

        retryWithExpectedResult[Option[StringDataPoint]](
          dataPoints.getLatestStringDataPoint(CogniteExternalId(stringTimeSeriesExternalId)),
          l2 => {
            l2.isDefined shouldBe true
            testStringDataPoints.toList should contain(l2.value)
          }
        )

        dataPoints.deleteRangeById(stringTimeSeriesId, start, end.plusMillis(1))
        retryWithExpectedResult[StringDataPointsByExternalIdResponse](
          dataPoints.queryStringsByExternalId(stringTimeSeriesExternalId, start, end.plusMillis(1)),
          pad => pad.datapoints should have size 0
        )
    }

    it should "support support query by externalId when ignoreUnknownIds=true" in {
      val doesNotExist = CogniteExternalId(s"does-not-exist-${UUID.randomUUID.toString}")
      dataPoints.getLatestStringDataPoints(Seq(doesNotExist), ignoreUnknownIds = true) shouldBe empty
      dataPoints.getLatestDataPoints(Seq(doesNotExist), ignoreUnknownIds = true) shouldBe empty
      dataPoints.query(Seq(doesNotExist), Instant.EPOCH, Instant.now, ignoreUnknownIds = true) shouldBe empty
      dataPoints.queryStrings(Seq(doesNotExist), Instant.EPOCH, Instant.now, ignoreUnknownIds = true) shouldBe empty
    }

    it should "support support query by internal id when ignoreUnknownIds=true" in {
      val doesNotExist = CogniteInternalId(9007199254740991L)
      dataPoints.getLatestStringDataPoints(Seq(doesNotExist), ignoreUnknownIds = true) shouldBe empty
      dataPoints.getLatestDataPoints(Seq(doesNotExist), ignoreUnknownIds = true) shouldBe empty
      dataPoints.queryByIds(Seq(doesNotExist.id), Instant.EPOCH, Instant.now, ignoreUnknownIds = true) shouldBe empty
      dataPoints.queryStrings(Seq(doesNotExist), Instant.EPOCH, Instant.now, ignoreUnknownIds = true) shouldBe empty
    }

    it should "be an error insert or delete string data points for non-existing time series" in {
      val unknownId = 991919L
      val thrown = the[CdpApiException] thrownBy dataPoints.insertStrings(CogniteInternalId(unknownId), testStringDataPoints)

      val itemsNotFound = thrown.missing.value
      val notFoundIds =
        itemsNotFound.map(jsonObj => jsonObj("id").value.asNumber.value.toLong.value)
      itemsNotFound should have size 1
      assert(notFoundIds.headOption.value === unknownId)
    }

    it should "be an error to delete string data points for non-existing time series" in {
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
