package com.cognite.sdk.scala.v1

import java.util.UUID

import com.cognite.sdk.scala.common.{DataPointsResourceBehaviors, SdkTest}
//import io.circe.generic.auto._

class DataPointsTest extends SdkTest with DataPointsResourceBehaviors[Long] {
  private val client = new GenericClient()(auth, sttpBackend)
  //import com.cognite.sdk.scala.common.ExtractorInstances._

  override def withTimeSeriesId(testCode: Long => Any): Unit = {
    val timeSeriesId = client.timeSeries.create(
      Seq(TimeSeries(name = s"data-points-test-${UUID.randomUUID().toString}"))
    ).unsafeBody.head.id
    try {
      val _ = testCode(timeSeriesId)
    } finally {
      client.timeSeries.deleteByIds(Seq(timeSeriesId)).unsafeBody
    }
  }

  override def withStringTimeSeriesId(testCode: Long => Any): Unit = {
    val timeSeriesId = client.timeSeries.create(
      Seq(TimeSeries(name = s"string-data-points-test-${UUID.randomUUID().toString}", isString = true))
    ).unsafeBody.head.id
    try {
      val _ = testCode(timeSeriesId)
    } finally {
      client.timeSeries.deleteByIds(Seq(timeSeriesId)).unsafeBody
    }
  }

  it should behave like dataPointsResource(client.dataPoints)
}
