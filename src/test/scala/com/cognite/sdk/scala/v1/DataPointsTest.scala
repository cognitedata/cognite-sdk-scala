package com.cognite.sdk.scala.v1

import java.util.UUID

import cats.{Functor, Id}
import com.cognite.sdk.scala.common.{DataPointsResourceBehaviors, SdkTest}

class DataPointsTest extends SdkTest with DataPointsResourceBehaviors[Long] {
  private val client = new GenericClient()(implicitly[Functor[Id]], auth, sttpBackend)

  override def withTimeSeriesId(testCode: Long => Any): Unit = {
    val timeSeriesId = client.timeSeries.create(
      Seq(TimeSeries(name = s"data-points-test-${UUID.randomUUID().toString}"))
    ).head.id
    try {
      val _ = testCode(timeSeriesId)
    } finally {
      client.timeSeries.deleteByIds(Seq(timeSeriesId))
    }
  }

  override def withStringTimeSeriesId(testCode: Long => Any): Unit = {
    val timeSeriesId = client.timeSeries.create(
      Seq(TimeSeries(name = s"string-data-points-test-${UUID.randomUUID().toString}", isString = true))
    ).head.id
    try {
      val _ = testCode(timeSeriesId)
    } finally {
      client.timeSeries.deleteByIds(Seq(timeSeriesId))
    }
  }

  it should behave like dataPointsResource(client.dataPoints)
}
