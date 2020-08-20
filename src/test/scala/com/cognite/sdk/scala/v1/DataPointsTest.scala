package com.cognite.sdk.scala.v1

import java.time.Instant
import java.util.UUID

import com.cognite.sdk.scala.common.{CdpApiException, DataPointsResourceBehaviors, SdkTestSpec}

class DataPointsTest extends SdkTestSpec with DataPointsResourceBehaviors {
  override def withTimeSeries(testCode: TimeSeries => Any): Unit = {
    val name = Some(s"data-points-test-${UUID.randomUUID().toString}")
    val timeSeries = client.timeSeries
      .createFromRead(
        Seq(TimeSeries(name = name, externalId = name))
      )
      .head
    try {
      val _ = testCode(timeSeries)
    } catch {
      case t: Throwable => throw t
    } finally {
      client.timeSeries.deleteByIds(Seq(timeSeries.id))
    }
  }

  it should behave like dataPointsResource(client.dataPoints)

  it should "be possible to correctly query aggregate values" in {
    val aggregates = client.dataPoints.queryAggregatesById(
      54577852743225L,
      Instant.ofEpochMilli(0L),
      Instant.ofEpochMilli(1553795183461L),
      "1d",
      Seq("average", "stepInterpolation")
    )
    val averages = aggregates("average").head.datapoints
    val stepInterpolations = aggregates("stepInterpolation").head.datapoints
    averages.map(_.timestamp).tail should contain theSameElementsInOrderAs stepInterpolations.map(_.timestamp)
    averages.last.value should equal(2.7051279586700723)

    val limit = 10
    val aggregatesWithLimit = client.dataPoints.queryAggregatesById(
      54577852743225L,
      Instant.ofEpochMilli(0L),
      Instant.ofEpochMilli(1553795183461L),
      "1d",
      Seq("average", "stepInterpolation"),
      Some(limit)
    )
    val averagesWithLimit = aggregatesWithLimit("average").head.datapoints
    val stepInterpolationsWithLimit = aggregatesWithLimit("stepInterpolation").head.datapoints
    averagesWithLimit.size should be <= limit
    stepInterpolationsWithLimit.size should be <= limit
    averagesWithLimit.head.value should equal(averages.head.value)
    stepInterpolationsWithLimit.head.value should equal(stepInterpolations.head.value)

    val aggregatesWithZeroLimit = client.dataPoints.queryAggregatesById(
      54577852743225L,
      Instant.ofEpochMilli(0L),
      Instant.ofEpochMilli(1553795183461L),
      "1d",
      Seq("average", "stepInterpolation"),
      Some(0)
    )
    aggregatesWithZeroLimit should equal (Map())

    assertThrows[CdpApiException] {
      // negative limit parameter is not allowed by the API
      client.dataPoints.queryAggregatesById(
        54577852743225L,
        Instant.ofEpochMilli(0L),
        Instant.ofEpochMilli(1553795183461L),
        "1d",
        Seq("average", "stepInterpolation"),
        Some(-123)
      )
    }

    val extAggregates = client.dataPoints.queryAggregatesByExternalId(
      "VAL_23_FIC_92543_06:Z.X.Value",
      Instant.ofEpochMilli(0L),
      Instant.ofEpochMilli(1553795183461L),
      "1d",
      Seq("average", "stepInterpolation")
    )
    extAggregates.keys should contain theSameElementsAs List("average", "stepInterpolation")
    val extAverages = extAggregates("average").head.datapoints
    val extStepInterpolation = extAggregates("stepInterpolation").head.datapoints
    extAverages.map(_.timestamp).tail should contain theSameElementsInOrderAs extStepInterpolation.map(_.timestamp)
    extAverages.map(_.timestamp) shouldBe sorted
    extStepInterpolation.map(_.timestamp) shouldBe sorted
    aggregates.keys should contain only (Seq("average", "stepInterpolation"):_*)
    extAverages.head.value should equal(2.7346077636587798)
    extAverages.last.value should equal(2.7051279586700723)
    extStepInterpolation.last.value should equal(2.8424909114837646)
    val aggregates2 = client.dataPoints.queryAggregatesById(
      54577852743225L,
      Instant.ofEpochMilli(0L),
      Instant.ofEpochMilli(1553795183461L),
      "1h",
      Seq("sum", "stepInterpolation")
    )
    val sums2 = aggregates2("sum").head.datapoints
    val stepInterpolation2 = aggregates2("stepInterpolation").head.datapoints
    sums2.head.value should equal(1883.142868757248)
    sums2.last.value should equal(1845.0451910495758)
    stepInterpolation2.head.value should equal(2.739926815032959)
    stepInterpolation2.last.value should equal(2.8009767532348633)

    val extAggregatesWithLimit = client.dataPoints.queryAggregatesByExternalId(
      "VAL_23_FIC_92543_06:Z.X.Value",
      Instant.ofEpochMilli(0L),
      Instant.ofEpochMilli(1553795183461L),
      "1d",
      Seq("average", "stepInterpolation"),
      Some(limit)
    )
    val extAveragesWithLimit = extAggregatesWithLimit("average").head.datapoints
    val extStepInterpolationWithLimit = extAggregatesWithLimit("stepInterpolation").head.datapoints
    extAveragesWithLimit.size should be <= limit
    extStepInterpolationWithLimit.size should be <= limit
    extAveragesWithLimit.head.value should equal(extAverages.head.value)
    extAveragesWithLimit.head.value should equal(averages.head.value)
    extAveragesWithLimit.last.value should equal(averagesWithLimit.last.value)
    extStepInterpolationWithLimit.head.value should equal(extStepInterpolation.head.value)
    extStepInterpolationWithLimit.last.value should equal(stepInterpolationsWithLimit.last.value)
    val extAggregatesWithZeroLimit = client.dataPoints.queryAggregatesByExternalId(
      "VAL_23_FIC_92543_06:Z.X.Value",
      Instant.ofEpochMilli(0L),
      Instant.ofEpochMilli(1553795183461L),
      "1d",
      Seq("average", "stepInterpolation"),
      Some(0)
    )
    extAggregatesWithZeroLimit should equal (Map())

    assertThrows[CdpApiException] {
      // negative limit parameter is not allowed by the API
      client.dataPoints.queryAggregatesByExternalId(
        "test__constant_74_with_noise",
        Instant.ofEpochMilli(0L),
        Instant.ofEpochMilli(1553795183461L),
        "1d",
        Seq("average", "stepInterpolation"),
        Some(-1)
      )
    }

    val extAggregates2 =
      client.dataPoints.queryAggregatesByExternalId(
        "VAL_23_FIC_92543_06:Z.X.Value",
        Instant.ofEpochMilli(0L),
        Instant.ofEpochMilli(1553795183461L),
        "1h",
        Seq("sum", "stepInterpolation")
      )
    val extSum2 = extAggregates2("sum").head.datapoints
    val extStepInterpolation2 = extAggregates2("stepInterpolation").head.datapoints
    extSum2.map(_.timestamp).tail should contain theSameElementsInOrderAs extStepInterpolation2.map(_.timestamp)
    aggregates2.keys should contain only (Seq("sum", "stepInterpolation"):_*)
    assertThrows[CdpApiException] {
      val _ = client.dataPoints.queryAggregatesById(
        54577852743225L,
        Instant.ofEpochMilli(0L),
        Instant.ofEpochMilli(1553795183461L),
        "1d",
        Seq("invalid aggregate1", "minx")
      )
    }
    assertThrows[CdpApiException] {
      val _ = client.dataPoints.queryAggregatesById(
        54577852743225L,
        Instant.ofEpochMilli(0L),
        Instant.ofEpochMilli(1553795183461L),
        "1d",
        Seq.empty
      )
    }
  }

  it should "be possible to query aggregate values with ignoreUnknownIds" in {
    val aggregates = client.dataPoints.queryAggregatesByIds(
      Seq(123L),
      Instant.ofEpochMilli(0L),
      Instant.ofEpochMilli(1553795183461L),
      "1d",
      Seq("average", "stepInterpolation"),
      ignoreUnknownIds = true
    )

    aggregates shouldBe empty
  }

  val sumsOnly = client.dataPoints.queryAggregatesById(
    54577852743225L,
    Instant.ofEpochMilli(0L),
    Instant.ofEpochMilli(1553795183461L),
    "1d",
    Seq("min")
  )
  sumsOnly.keys should contain theSameElementsAs List("min")

  it should "correctly decode an error response as json instead of protobuf" in {
    val missingId = 1345746392847240L
    val caught = intercept[CdpApiException] {
      client.dataPoints.queryById(
        missingId,
        Instant.ofEpochMilli(0L),
        Instant.ofEpochMilli(1553795183461L)
      )
    }
    caught.missing.get.head.toMap("id").toString() shouldEqual missingId.toString

    val sCaught = intercept[CdpApiException] {
      client.dataPoints.queryById(
        missingId,
        Instant.ofEpochMilli(0L),
        Instant.ofEpochMilli(1553795183461L)
      )
    }
    sCaught.missing.get.head.toMap("id").toString() shouldEqual missingId.toString

    val aggregateCaught = intercept[CdpApiException] {
      client.dataPoints.queryAggregatesById(
        missingId,
        Instant.ofEpochMilli(0L),
        Instant.ofEpochMilli(1553795183461L),
        "1d",
        Seq("average")
      )
    }
    aggregateCaught.missing.get.head.toMap("id").toString() shouldEqual missingId.toString
  }

}
