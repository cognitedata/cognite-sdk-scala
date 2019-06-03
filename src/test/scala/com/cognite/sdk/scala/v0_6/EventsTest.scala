package com.cognite.sdk.scala.v0_6

import com.cognite.sdk.scala.common.{ReadableResourceBehaviors, SdkTest}

class EventsTest extends SdkTest with ReadableResourceBehaviors {
  private val client = new Client()

  it should behave like readableResource(client.events, supportsMissingAndThrown = false)
  it should behave like writableResource(
    client.events,
    Seq(Event(description = Some("scala-sdk-read-example-1")), Event(description = Some("scala-sdk-read-example-2"))),
    Seq(CreateEvent(description = Some("scala-sdk-create-example-1")), CreateEvent(description = Some("scala-sdk-create-example-2"))),
    supportsMissingAndThrown = false
  )
}
