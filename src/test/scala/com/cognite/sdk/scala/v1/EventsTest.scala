package com.cognite.sdk.scala.v1

import java.time.Instant

import fs2._
import com.cognite.sdk.scala.common.{ReadBehaviours, SdkTest, SetValue, WritableBehaviors}

class EventsTest extends SdkTest with ReadBehaviours with WritableBehaviors {
  private val idsThatDoNotExist = Seq(999991L, 999992L)
  private val externalIdsThatDoNotExist = Seq("5PNii0w4GCDBvXPZ", "6VhKQqtTJqBHGulw")

  it should behave like readable(client.events)

  it should behave like partitionedReadable(client.events)

  it should behave like readableWithRetrieve(client.events, idsThatDoNotExist, supportsMissingAndThrown = true)

  it should behave like readableWithRetrieveByExternalId(client.events, externalIdsThatDoNotExist, supportsMissingAndThrown = true)

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

  it should behave like deletableWithIgnoreUnknownIds(
    client.events,
    Seq(
      Event(description = Some("scala-sdk-read-example-1"), externalId = Some(shortRandom())),
      Event(description = Some("scala-sdk-read-example-2"), externalId = Some(shortRandom()))
    ),
    idsThatDoNotExist
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

  it should behave like updatableById(
    client.events,
    eventsToCreate,
    Seq(EventUpdate(description = Some(SetValue("scala-sdk-update-1-1"))), EventUpdate(description = Some(SetValue("scala-sdk-update-2-1")))),
    (readEvents: Seq[Event], updatedEvents: Seq[Event]) => {
      assert(readEvents.size == updatedEvents.size)
      assert(updatedEvents.zip(readEvents).forall { case (updated, read) =>  updated.description.get == s"${read.description.get}-1" })
      ()
    }
  )

  it should behave like updatableByExternalId(
    client.events,
    Seq(Event(description = Some("description-1"), externalId = Some("update-1-externalId")),
      Event(description = Some("description-2"), externalId = Some("update-2-externalId"))),
    Map("update-1-externalId" -> EventUpdate(description = Some(SetValue("description-1-1"))),
      "update-2-externalId" -> EventUpdate(description = Some(SetValue("description-2-1")))),
    (readEvents: Seq[Event], updatedEvents: Seq[Event]) => {
      assert(readEvents.size == updatedEvents.size)
      assert(updatedEvents.zip(readEvents).forall { case (updated, read) =>
        updated.description.getOrElse("") == s"${read.description.getOrElse("")}-1" })
      assert(updatedEvents.zip(readEvents).forall { case (updated, read) => updated.externalId == read.externalId })
      ()
    }
  )

  it should "support updating by id" in {
    val createEvents = client.events.createFromRead(
      Seq(Event(description = Some("description-1")), Event(description = Some("description-2"))))
    import com.cognite.sdk.scala.common.SetValue
    val updatedEvents = client.events.updateById(
      Map(createEvents.head.id -> EventUpdate(description = Some(SetValue("description-1-1"))),
        createEvents.tail.head.id -> EventUpdate(description = Some(SetValue("description-2-1")))))
    assert(updatedEvents.zip(createEvents).forall {
      case (updated, read) => updated.description.get == s"${read.description.get}-1" })
    assert(updatedEvents.zip(createEvents).forall { case (updated, read) => updated.id == read.id })
  }

  it should "support filter" in {
    val createdTimeFilterResults = client.events
      .filter(
        EventsFilter(
          createdTime = Some(
            TimeRange(Instant.ofEpochMilli(1500510008838L), Instant.ofEpochMilli(1550300000000L))
          )
        )
      )
      .compile
      .toList
    assert(createdTimeFilterResults.length == 3)
    val createdTimeFilterPartitionsResults = client.events
      .filterPartitions(
        EventsFilter(
          createdTime = Some(
            TimeRange(Instant.ofEpochMilli(1500510008838L), Instant.ofEpochMilli(1550300000000L))
          )
        ), 20
      )
      .fold(Stream.empty)(_ ++ _)
      .compile
      .toList
    assert(createdTimeFilterPartitionsResults.length == 3)
    val createdTimeFilterResultsWithLimit = client.events
      .filter(
        EventsFilter(
          createdTime = Some(
            TimeRange(Instant.ofEpochMilli(1500510008838L), Instant.ofEpochMilli(1550300000000L))
          )
        ),
        limit = Some(1)
      )
      .compile
      .toList
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
                  Instant.ofEpochMilli(1500510008838L),
                  Instant.ofEpochMilli(1550300000000L)
                )
              )
            )
          )
        )
      )
    assert(createdTimeSearchResults.length == 3)
    val subtypeCreatedTimeSearchResults = client.events
      .search(
        EventsQuery(
          filter = Some(
            EventsFilter(
              createdTime = Some(
                TimeRange(
                  Instant.ofEpochMilli(1500510008838L),
                  Instant.ofEpochMilli(1550300000000L)
                )
              ),
              `type` = Some("test"),
              subtype = Some("test1")
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
                Some(TimeRange(Instant.ofEpochMilli(0), Instant.ofEpochMilli(1549011100999L)))
            )
          ),
          search = Some(
            EventsSearch(
              description = Some("wjoel")
            )
          )
        )
      )
    assert(searchResults.length == 1)
    val searchResults2 = client.events
      .search(
        EventsQuery(
          filter = Some(
            EventsFilter(
              createdTime =
                Some(TimeRange(Instant.ofEpochMilli(0), Instant.ofEpochMilli(1552395929199L)))
            )
          ),
          search = Some(
            EventsSearch(
              description = Some("test event wjoel")
            )
          )
        )
      )
    assert(searchResults2.length == 2)
    val limitSearchResults = client.events
      .search(
        EventsQuery(
          limit = 1,
          filter = Some(
            EventsFilter(
              createdTime =
                Some(TimeRange(Instant.ofEpochMilli(0), Instant.ofEpochMilli(1552395929199L)))
            )
          ),
          search = Some(
            EventsSearch(
              description = Some("description")
            )
          )
        )
      )
    assert(limitSearchResults.length == 1)
  }
}
