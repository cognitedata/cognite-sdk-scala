package com.cognite.sdk.scala.v1

import java.time.Instant

import fs2._
import com.cognite.sdk.scala.common.{ReadBehaviours, RetryWhile, SdkTestSpec, SetNull, SetValue, WritableBehaviors}

class EventsTest extends SdkTestSpec with ReadBehaviours with WritableBehaviors with RetryWhile {
  private val idsThatDoNotExist = Seq(999991L, 999992L)
  private val externalIdsThatDoNotExist = Seq("5PNii0w4GCDBvXPZ", "6VhKQqtTJqBHGulw")

  // Use greenfieldClient for readable tests since publicdata has a lot of events,
  // so it's painfully slow to read them all sequentially.
  it should behave like readable(greenfieldClient.events)

  it should behave like partitionedReadable(greenfieldClient.events)

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
    Event(description = Some("scala-sdk-update-2"), `type` = Some("test"), subtype = Some("test")),
    Event(description = Some("scala-sdk-update-3"), `type` = Some("test"), dataSetId = Some(testDataSet.id))
  )
  private val eventUpdates = Seq(
    Event(description = Some("scala-sdk-update-1-1"), `type` = Some("testA"), subtype = Some(null)), // scalastyle:ignore null
    Event(
      description = Some("scala-sdk-update-2-1"),
      `type` = Some("testA"),
      subtype = Some("test-1"),
      dataSetId = Some(testDataSet.id)
    ),
    Event(description = Some("scala-sdk-update-3-1"))
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
      val dataSets = updatedEvents.map(_.dataSetId)
      assert(List(None, Some(testDataSet.id), Some(testDataSet.id)) === dataSets)
      ()
    }
  )

  it should behave like updatableById(
    client.events,
    eventsToCreate,
    Seq(
      EventUpdate(description = Some(SetValue("scala-sdk-update-1-1"))),
      EventUpdate(description = Some(SetValue("scala-sdk-update-2-1"))),
      EventUpdate(description = Some(SetValue("scala-sdk-update-3-1")), dataSetId = Some(SetNull()))
    ),
    (readEvents: Seq[Event], updatedEvents: Seq[Event]) => {
      assert(readEvents.size == updatedEvents.size)
      assert(updatedEvents.zip(readEvents).forall { case (updated, read) =>  updated.description.get == s"${read.description.get}-1" })
      val dataSets = updatedEvents.map(_.dataSetId)
      assert(List(None, None, None) === dataSets)
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
            TimeRange(Instant.ofEpochMilli(1581098334114L), Instant.ofEpochMilli(1581098400000L))
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
            TimeRange(Instant.ofEpochMilli(1581098334114L), Instant.ofEpochMilli(1581098400000L))
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
            TimeRange(Instant.ofEpochMilli(1581098334114L), Instant.ofEpochMilli(1581098400000L))
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
                  Instant.ofEpochMilli(1581098334114L),
                  Instant.ofEpochMilli(1581098400000L)
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
                  Instant.ofEpochMilli(1581098334114L),
                  Instant.ofEpochMilli(1581102000000L)
                )
              ),
              `type` = Some("test-data-populator"),
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
                Some(TimeRange(Instant.ofEpochMilli(1581098334114L), Instant.ofEpochMilli(1581102000000L)))
            )
          ),
          search = Some(
            EventsSearch(
              description = Some("wjoel")
            )
          )
        )
      )
    assert(searchResults.length == 2)
    val searchResults2 = client.events
      .search(
        EventsQuery(
          filter = Some(
            EventsFilter(
              createdTime =
                Some(TimeRange(Instant.ofEpochMilli(1581098334114L), Instant.ofEpochMilli(1581102000000L)))
            )
          ),
          search = Some(
            EventsSearch(
              description = Some("test joel")
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
                Some(TimeRange(Instant.ofEpochMilli(1581098334114L), Instant.ofEpochMilli(1581102000000L)))
            )
          ),
          search = Some(
            EventsSearch(
              description = Some("wjoel")
            )
          )
        )
      )
    assert(limitSearchResults.length == 1)
  }


  it should "support search with dataSetIds" in {
    val created = client.events.createFromRead(eventsToCreate)
    try {
      val createdTimes = created.map(_.createdTime)
      val foundItems = retryWithExpectedResult[Seq[Event]](
        client.events.search(EventsQuery(Some(EventsFilter(
          dataSetIds = Some(Seq(CogniteInternalId(testDataSet.id))),
          createdTime = Some(TimeRange(
            min = createdTimes.min,
            max = createdTimes.max
          ))
        )))),
        a => a should not be empty
      )
      foundItems.map(_.dataSetId) should contain only Some(testDataSet.id)
      created.filter(_.dataSetId.isDefined).map(_.id) should contain only (foundItems.map(_.id): _*)
    } finally {
      client.events.deleteByIds(created.map(_.id))
    }
  }
}
