package com.cognite.sdk.scala.v06

import com.cognite.sdk.scala.common.{ReadableResourceBehaviors, SdkTest, WritableResourceBehaviors}

class TimeSeriesTest extends SdkTest with ReadableResourceBehaviors with WritableResourceBehaviors {
  private val client = new GenericClient()(auth, sttpBackend)
  private val idsThatDoNotExist = Seq(999991L, 999992L)

  it should behave like readableResource(client.timeSeries)
  it should behave like readableResourceWithRetrieve(client.timeSeries, idsThatDoNotExist, supportsMissingAndThrown = false)
  it should behave like writableResource(
    client.timeSeries,
    Seq(TimeSeries(name = "scala-sdk-read-example-11"), TimeSeries(name = "scala-sdk-read-example-12")),
    Seq(CreateTimeSeries(name = "scala-sdk-create-example-11"), CreateTimeSeries(name = "scala-sdk-create-example-12")),
    idsThatDoNotExist,
    supportsMissingAndThrown = false
  )
}
