package com.cognite.sdk.scala.common

import java.time.Instant
import java.util.UUID

import com.cognite.sdk.scala.v1.{StringDataPointsByExternalIdResponse, StringDataPointsByIdResponse, TimeSeries}
import com.cognite.sdk.scala.v1.resources.DataPointsResource
import com.softwaremill.sttp.Id
import org.scalatest.{FlatSpec, Matchers}

trait StringDataPointsResourceBehaviors extends Matchers with RetryWhile { this: FlatSpec =>
  private val startTime = System.currentTimeMillis()
  private val start = Instant.ofEpochMilli(startTime)
  private val endTime = startTime + 20*1000
  private val end = Instant.ofEpochMilli(endTime)
  private val testStringDataPoints = (startTime to endTime by 1000).map(t =>
    StringDataPoint(Instant.ofEpochMilli(t), math.random.toString))

  def withStringTimeSeries(testCode: TimeSeries => Any): Unit

  // scalastyle:off
  def stringDataPointsResource(dataPoints: DataPointsResource[Id]): Unit = {
    it should "be possible to insert and delete string data points" in withStringTimeSeries {
      stringTimeSeries =>
      val stringTimeSeriesId = stringTimeSeries.id
        val stringTimeSeriesExternalId = stringTimeSeries.externalId.get
        dataPoints.insertStringsById(stringTimeSeriesId, testStringDataPoints)

        retryWithExpectedResult[StringDataPointsByIdResponse](
          dataPoints.queryStringsById(stringTimeSeriesId, start, end.plusMillis(1)),
          p => p.datapoints should have size testStringDataPoints.size.toLong
        )

        retryWithExpectedResult[Option[StringDataPoint]](
          dataPoints.getLatestStringDataPointById(stringTimeSeriesId),
          dp => {
            dp.isDefined shouldBe true
            testStringDataPoints.toList should contain(dp.get)
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

        val resultId2: Seq[_] = dataPoints.queryStringsByIds(Seq(stringTimeSeriesId), start, end.plusMillis(1))
        resultId2.length shouldBe 1
        resultId2.head shouldBe resultId

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
        resultExternalId2.head shouldBe resultExternalId

        retryWithExpectedResult[Option[StringDataPoint]](
          dataPoints.getLatestStringDataPointByExternalId(stringTimeSeriesExternalId),
          l2 => {
            l2.isDefined shouldBe true
            testStringDataPoints.toList should contain(l2.get)
          }
        )

        dataPoints.deleteRangeById(stringTimeSeriesId, start, end.plusMillis(1))
        retryWithExpectedResult[StringDataPointsByExternalIdResponse](
          dataPoints.queryStringsByExternalId(stringTimeSeriesExternalId, start, end.plusMillis(1)),
          pad => pad.datapoints should have size 0
        )
    }

    it should "support support query by externalId when ignoreUnknownIds=true" in {
      val doesNotExist = "does-not-exist-" + UUID.randomUUID
      dataPoints.getLatestStringDataPointByExternalIds(Seq(doesNotExist), ignoreUnknownIds = true) shouldBe empty
      dataPoints.getLatestDataPointsByExternalIds(Seq(doesNotExist), ignoreUnknownIds = true) shouldBe empty
      dataPoints.queryByExternalIds(Seq(doesNotExist), Instant.EPOCH, Instant.now, ignoreUnknownIds = true) shouldBe empty
      dataPoints.queryStringsByExternalIds(Seq(doesNotExist), Instant.EPOCH, Instant.now, ignoreUnknownIds = true) shouldBe empty
    }

    it should "support support query by internal id when ignoreUnknownIds=true" in {
      val doesNotExist = 9007199254740991L
      dataPoints.getLatestStringDataPointByIds(Seq(doesNotExist), ignoreUnknownIds = true) shouldBe empty
      dataPoints.getLatestDataPointsByIds(Seq(doesNotExist), ignoreUnknownIds = true) shouldBe empty
      dataPoints.queryByIds(Seq(doesNotExist), Instant.EPOCH, Instant.now, ignoreUnknownIds = true) shouldBe empty
      dataPoints.queryStringsByIds(Seq(doesNotExist), Instant.EPOCH, Instant.now, ignoreUnknownIds = true) shouldBe empty
    }
  }
}
