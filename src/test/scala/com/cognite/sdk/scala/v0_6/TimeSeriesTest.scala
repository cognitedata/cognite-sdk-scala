package com.cognite.sdk.scala.v0_6

import com.cognite.sdk.scala.common.{ReadableResourceBehaviors, SdkTest}

class TimeSeriesTest extends SdkTest with ReadableResourceBehaviors {
  private val client = new Client()

  it should behave like readableResource(client.timeSeries, supportsMissingAndThrown = false)
  it should behave like writableResource(
    client.timeSeries,
    Seq(TimeSeries(name = "scala-sdk-read-example-1"), TimeSeries(name = "scala-sdk-read-example-2")),
    Seq(CreateTimeSeries(name = "scala-sdk-create-example-1"), CreateTimeSeries(name = "scala-sdk-create-example-2")),
    supportsMissingAndThrown = false
  )
}
