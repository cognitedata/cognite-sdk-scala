package com.cognite.sdk.scala.v1

import java.time.Instant

import cats.{Functor, Id}
import com.cognite.sdk.scala.common.{ReadBehaviours, SdkTest, WritableBehaviors}

class EventsTest extends SdkTest with ReadBehaviours with WritableBehaviors {
  private val client = new GenericClient()(implicitly[Functor[Id]], auth, sttpBackend)
  private val idsThatDoNotExist = Seq(999991L, 999992L)
  private val externalIdsThatDoNotExist = Seq("5PNii0w4GCDBvXPZ", "6VhKQqtTJqBHGulw")

  it should behave like readable(client.events)

  it should behave like readableWithRetrieve(client.events, idsThatDoNotExist, supportsMissingAndThrown = true)

  it should behave like writable(
    client.events,
    Seq(Event(description = Some("scala-sdk-read-example-1")), Event(description = Some("scala-sdk-read-example-2"))),
    Seq(EventCreate(description = Some("scala-sdk-create-example-1")), EventCreate(description = Some("scala-sdk-create-example-2"))),
    idsThatDoNotExist,
    supportsMissingAndThrown = true
  )

  it should behave like writableWithExternalId(
    client.events,
    Seq(
      Event(description = Some("scala-sdk-read-example-1"), externalId = Some(shortRandom())),
      Event(description = Some("scala-sdk-read-example-2"), externalId = Some(shortRandom()))
    ),
    Seq(
      EventCreate(description = Some("scala-sdk-read-example-1"), externalId = Some(shortRandom())),
      EventCreate(description = Some("scala-sdk-read-example-2"), externalId = Some(shortRandom()))
    ),
    externalIdsThatDoNotExist,
    supportsMissingAndThrown = true
  )

  private val eventsToCreate = Seq(
    Event(description = Some("scala-sdk-update-1"), `type` = Some("test"), subtype = Some("test")),
    Event(description = Some("scala-sdk-update-2"), `type` = Some("test"), subtype = Some("test"))
  )
  private val eventUpdates = Seq(
    Event(description = Some("scala-sdk-update-1-1"), `type` = Some("testA"), subtype = Some(null)), // scalastyle:ignore null
    Event(
      description = Some("scala-sdk-update-2-1"),
      `type` = Some("testA"),
      subtype = Some("test-1")
    )
  )

  it should behave like updatable(
    client.events,
    eventsToCreate,
    eventUpdates,
    (id: Long, item: Event) => item.copy(id = id),
    (a: Event, b: Event) => { a == b },
    (readEvents: Seq[Event], updatedEvents: Seq[Event]) => {
      assert(eventsToCreate.size == eventUpdates.size)
      assert(readEvents.size == eventsToCreate.size)
      assert(readEvents.size == updatedEvents.size)
      assert(updatedEvents.zip(readEvents).forall { case (updated, read) =>
        updated.description.nonEmpty &&
          read.description.nonEmpty &&
          updated.description.forall { description => description == s"${read.description.get}-1"}
      })
      assert(readEvents.head.subtype.isDefined)
      assert(updatedEvents.head.subtype.isEmpty)
      assert(updatedEvents(1).subtype == eventUpdates(1).subtype)
      ()
    }
  )

  it should "support filter" in {
    val createdTimeFilterResults = client.events
      .filter(
        EventsFilter(
          createdTime = Some(
            TimeRange(Instant.ofEpochMilli(1541510008838L), Instant.ofEpochMilli(1541515508838L))
          )
        )
      )
    assert(createdTimeFilterResults.length == 11)
    val createdTimeFilterResultsWithLimit = client.events
      .filterWithLimit(
        EventsFilter(
          createdTime = Some(
            TimeRange(Instant.ofEpochMilli(1541510008838L), Instant.ofEpochMilli(1541515508838L))
          )
        ),
        1
      )
    assert(createdTimeFilterResultsWithLimit.length == 1)
  }

  it should "support search" in {
    val createdTimeSearchResults = client.events
      .search(
        EventsQuery(
          filter = Some(
            EventsFilter(
              createdTime = Some(
                TimeRange(
                  Instant.ofEpochMilli(1541510008838L),
                  Instant.ofEpochMilli(1541515508838L)
                )
              )
            )
          )
        )
      )
    assert(createdTimeSearchResults.length == 11)
    val subtypeCreatedTimeSearchResults = client.events
      .search(
        EventsQuery(
          filter = Some(
            EventsFilter(
              createdTime = Some(
                TimeRange(
                  Instant.ofEpochMilli(1541510008838L),
                  Instant.ofEpochMilli(1541515508838L)
                )
              ),
              `type` = Some("Workorder"),
              subtype = Some("Foo")
            )
          )
        )
      )
    assert(subtypeCreatedTimeSearchResults.length == 1)
    val searchResults = client.events
      .search(
        EventsQuery(
          filter = Some(
            EventsFilter(
              createdTime =
                Some(TimeRange(Instant.ofEpochMilli(0), Instant.ofEpochMilli(1541515508838L)))
            )
          ),
          search = Some(
            EventsSearch(
              description = Some("description")
            )
          )
        )
      )
    assert(searchResults.length == 3)
    val searchResults2 = client.events
      .search(
        EventsQuery(
          filter = Some(
            EventsFilter(
              createdTime =
                Some(TimeRange(Instant.ofEpochMilli(0), Instant.ofEpochMilli(1552395929193L)))
            )
          ),
          search = Some(
            EventsSearch(
              description = Some("description")
            )
          )
        )
      )
    assert(searchResults2.length == 7)
    val limitSearchResults = client.events
      .search(
        EventsQuery(
          limit = 3,
          filter = Some(
            EventsFilter(
              createdTime =
                Some(TimeRange(Instant.ofEpochMilli(0), Instant.ofEpochMilli(1552395929193L)))
            )
          ),
          search = Some(
            EventsSearch(
              description = Some("description")
            )
          )
        )
      )
    assert(limitSearchResults.length == 3)
  }
}
