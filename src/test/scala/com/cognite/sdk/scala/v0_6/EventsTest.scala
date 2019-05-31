package com.cognite.sdk.scala.v0_6

import com.cognite.sdk.scala.common.SdkTest

class EventsTest extends SdkTest {
  it should "be possible to retrieve an event" in {
    val client = new Client()
    val events = client.events.read()
    println(events.unsafeBody.items.map(_.description.toString).mkString(",\n")) // scalastyle:ignore
    println(s"${events.unsafeBody.items.length} events") // scalastyle:ignore
  }

  it should "fetch all events" in {
    val client = new Client()
    val events = client.events.readAll().take(10)
    val f = events.flatMap(_.toIterator).take(10000)
    println(f.length) // scalastyle:ignore
  }

  it should "be possible to write an event" in {
    val client = new Client()
    val events = client.events.create(
      Seq(Event(description = Some("cognite-scala-sdk"), `type` = Some("cognite-scala-sdk")))
    )
    println("wrote events: ") // scalastyle:ignore
    println(events.unsafeBody.map(_.toString).mkString(", ")) // scalastyle:ignore
  }
}
