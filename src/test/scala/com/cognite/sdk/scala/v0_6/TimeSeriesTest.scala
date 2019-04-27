package com.cognite.sdk.scala.v0_6

import com.cognite.sdk.scala.common.SdkTest

class TimeSeriesTest extends SdkTest {
  it should "be possible to retrieve a time series" in {
    val client = new Client()
    val timeSeries = client.timeSeries.read()
    println(timeSeries.unsafeBody.items.map(ts => s"${ts.name}: ${ts.description.getOrElse("[none]")}").mkString(",\n")) // scalastyle:ignore
    println(s"${timeSeries.unsafeBody.items.length} time series") // scalastyle:ignore
  }

  it should "fetch all timeseries" in {
    val client = new Client()
    val timeSeries = client.timeSeries.readAll().take(10)
    val f = timeSeries.flatMap(_.toIterator).take(10000)
    println(f.length) // scalastyle:ignore
  }

  it should "be possible to create a new time series" in {
    val r = scala.util.Random
    val client = new Client()
    val id = r.nextInt().toString
    val timeSeries = client.timeSeries.write(Seq(TimeSeries(name = s"cognite-scala-sdk-$id", description = Some(s"test id $id"))))
    println("Created time series: ") // scalastyle:ignore
    println(timeSeries.unsafeBody.map(_.toString).mkString(", ")) // scalastyle:ignore
  }
}
