package com.cognite.sdk.scala.v06

import com.cognite.sdk.scala.common.{ReadBehaviours, SdkTest, WritableBehaviors}
import com.cognite.sdk.scala.v06.resources.{CreateTimeSeries, TimeSeries}

class TimeSeriesTest extends SdkTest with ReadBehaviours with WritableBehaviors {
  private val client = new GenericClient()(auth, sttpBackend)
  private val idsThatDoNotExist = Seq(999991L, 999992L)

  it should behave like readable(client.timeSeries)
  it should behave like readableWithRetrieve(client.timeSeries, idsThatDoNotExist, supportsMissingAndThrown = false)
  it should behave like writable(
    client.timeSeries,
    Seq(TimeSeries(name = "scala-sdk-read-example-11"), TimeSeries(name = "scala-sdk-read-example-12")),
    Seq(CreateTimeSeries(name = "scala-sdk-create-example-11"), CreateTimeSeries(name = "scala-sdk-create-example-12")),
    idsThatDoNotExist,
    supportsMissingAndThrown = false
  )
}
