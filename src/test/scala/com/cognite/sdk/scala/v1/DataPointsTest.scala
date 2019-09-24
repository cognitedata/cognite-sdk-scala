package com.cognite.sdk.scala.v1

import java.util.UUID
import com.cognite.sdk.scala.common.{CdpApiException, DataPointsResourceBehaviors, SdkTest}

class DataPointsTest extends SdkTest with DataPointsResourceBehaviors[Long] {

  override def withTimeSeriesId(testCode: Long => Any): Unit = {
    val timeSeriesId = client.timeSeries.createFromRead(
        Seq(TimeSeries(name = s"data-points-test-${UUID.randomUUID().toString}"))
    ).head.id
    try {
      val _ = testCode(timeSeriesId)
    } finally {
      client.timeSeries.deleteByIds(Seq(timeSeriesId))
    }
  }

  it should behave like dataPointsResource(client.dataPoints)

  it should "be possible to correctly query aggregate values" in {
    val aggregates = client.dataPoints.queryAggregatesById(
      1580330145648L,
      0L,
      1564272000000L,
      "1d",
      Seq("average", "stepInterpolation")
    )
    val averages = aggregates("average")
    val stepInterpolations = aggregates("stepInterpolation")
    averages.map(_.timestamp).tail should contain theSameElementsInOrderAs stepInterpolations.map(_.timestamp)
    averages.last.value should equal(73.9999423351708)
    val extAggregates = client.dataPoints.queryAggregatesByExternalId(
      "test__constant_74_with_noise",
      0L,
      1564272000000L,
      "1d",
      Seq("average", "stepInterpolation")
    )
    val extAverages = extAggregates("average")
    val extStepInterpolation = extAggregates("stepInterpolation")
    extAverages.map(_.timestamp).tail should contain theSameElementsInOrderAs extStepInterpolation.map(_.timestamp)
    extAverages.map(_.timestamp) shouldBe sorted
    extStepInterpolation.map(_.timestamp) shouldBe sorted
    aggregates.keys should contain only("average", "stepInterpolation")
    extAverages.head.value should equal(74.00018450606277)
    extAverages.last.value should equal(73.9999423351708)
    extAverages.head.value should equal(74.00018450606277)
    extStepInterpolation.last.value should equal(74.07992673539263)
    val aggregates2 = client.dataPoints.queryAggregatesById(
      1580330145648L,
      0L,
      1564272000000L,
      "1h",
      Seq("sum", "stepInterpolation")
    )
    val sums2 = aggregates2("sum")
    val stepInterpolation2 = aggregates2("stepInterpolation")
    sums2.head.value should equal(65415.958570785)
    sums2.last.value should equal(253965.25002673318)
    stepInterpolation2.head.value should equal(73.92400373499633)
    stepInterpolation2.last.value should equal(74.04062931032483)
    val extAggregates2 =
      client.dataPoints.queryAggregatesByExternalId(
        "test__constant_74_with_noise",
        0L,
        1564272000000L,
        "1h",
        Seq("sum", "stepInterpolation")
      )
    val extSum2 = extAggregates2("sum")
    val extStepInterpolation2 = extAggregates2("stepInterpolation")
    extSum2.map(_.timestamp).tail should contain theSameElementsInOrderAs extStepInterpolation2.map(_.timestamp)
    aggregates2.keys should contain only("sum", "stepInterpolation")
    assertThrows[CdpApiException] {
      val _ = client.dataPoints.queryAggregatesById(
        1580330145648L,
        0L,
        1564272000000L,
        "1d",
        Seq("invalid aggregate1", "minx")
      )
    }
    assertThrows[CdpApiException] {
      val _ = client.dataPoints.queryAggregatesById(
        1580330145648L,
        0L,
        1564272000000L,
        "1d",
        Seq.empty
      )
    }
  }

  it should "correctly decode an error response as json instead of protobuf" in {
    val missingId = 1345746392847240L
    val caught = intercept[CdpApiException] {
      client.dataPoints.queryById(
        missingId,
        0L,
        1564272000000L
      )
    }
    caught.missing.get.head.toMap("id").toString() shouldEqual missingId.toString

    val sCaught = intercept[CdpApiException] {
      client.dataPoints.queryById(
        missingId,
        0L,
        1564272000000L
      )
    }
    sCaught.missing.get.head.toMap("id").toString() shouldEqual missingId.toString

    val aggregateCaught = intercept[CdpApiException] {
      client.dataPoints.queryAggregatesById(
        missingId,
        0L,
        1564272000000L,
        "1d",
        Seq("average")
      )
    }
    aggregateCaught.missing.get.head.toMap("id").toString() shouldEqual missingId.toString
  }

}
