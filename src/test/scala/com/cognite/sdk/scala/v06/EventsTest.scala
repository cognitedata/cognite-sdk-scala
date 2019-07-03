package com.cognite.sdk.scala.v06

import com.cognite.sdk.scala.common.{ReadBehaviours, SdkTest, WritableBehaviors}
import com.cognite.sdk.scala.v06.resources.{CreateEvent, Event}

class EventsTest extends SdkTest with ReadBehaviours with WritableBehaviors {
  private val client = new GenericClient()(auth, sttpBackend)
  private val idsThatDoNotExist = Seq(999991L, 999992L)

  it should behave like readable(client.events)
  it should behave like readableWithRetrieve(client.events, idsThatDoNotExist, supportsMissingAndThrown = false)
  it should behave like writable(
    client.events,
    Seq(Event(description = Some("scala-sdk-read-example-1")), Event(description = Some("scala-sdk-read-example-2"))),
    Seq(CreateEvent(description = Some("scala-sdk-create-example-1")), CreateEvent(description = Some("scala-sdk-create-example-2"))),
    idsThatDoNotExist,
    supportsMissingAndThrown = false
  )
}
