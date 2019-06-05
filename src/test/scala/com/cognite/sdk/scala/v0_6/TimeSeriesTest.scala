package com.cognite.sdk.scala.v0_6

import com.cognite.sdk.scala.common.{ReadableResourceBehaviors, SdkTest}

class TimeSeriesTest extends SdkTest with ReadableResourceBehaviors {
  private val client = new Client()

  it should behave like readableResource(client.timeSeries, supportsMissingAndThrown = false)
  it should behave like writableResource(
    client.timeSeries,
    Seq(TimeSeries(name = "scala-sdk-read-example-11"), TimeSeries(name = "scala-sdk-read-example-12")),
    Seq(CreateTimeSeries(name = "scala-sdk-create-example-11"), CreateTimeSeries(name = "scala-sdk-create-example-12")),
    supportsMissingAndThrown = false
  )
}
