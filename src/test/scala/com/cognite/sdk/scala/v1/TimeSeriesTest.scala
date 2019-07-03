package com.cognite.sdk.scala.v1

import com.cognite.sdk.scala.common.{ReadBehaviours, SdkTest, WritableBehaviors}
import com.softwaremill.sttp.Id

class TimeSeriesTest extends SdkTest with ReadBehaviours with WritableBehaviors {
  private val client = new GenericClient[Id, Nothing]()(auth, sttpBackend)
  private val idsThatDoNotExist = Seq(999991L, 999992L)

  it should behave like readable(client.timeSeries)
  it should behave like readableWithRetrieve(client.timeSeries, idsThatDoNotExist, supportsMissingAndThrown = true)
  it should behave like writable(
    client.timeSeries,
    Seq(TimeSeries(name = "scala-sdk-read-example-1"), TimeSeries(name = "scala-sdk-read-example-2")),
    Seq(CreateTimeSeries(name = "scala-sdk-create-example-1"), CreateTimeSeries(name = "scala-sdk-create-example-2")),
    idsThatDoNotExist,
    supportsMissingAndThrown = true
  )
}
