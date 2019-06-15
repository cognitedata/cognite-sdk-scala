package com.cognite.sdk.scala.v1

import com.cognite.sdk.scala.common.{ReadableResourceBehaviors, SdkTest, WritableResourceBehaviors}

class EventsTest extends SdkTest with ReadableResourceBehaviors with WritableResourceBehaviors {
  private val client = new GenericClient()(auth, backend)
  private val idsThatDoNotExist = Seq(999991L, 999992L)
  it should behave like readableResource(client.events)
  it should behave like readableResourceWithRetrieve(client.events, idsThatDoNotExist, supportsMissingAndThrown = true)
  it should behave like writableResource(
    client.events,
    Seq(Event(description = Some("scala-sdk-read-example-1")), Event(description = Some("scala-sdk-read-example-2"))),
    Seq(CreateEvent(description = Some("scala-sdk-create-example-1")), CreateEvent(description = Some("scala-sdk-create-example-2"))),
    idsThatDoNotExist,
    supportsMissingAndThrown = true
  )
}
