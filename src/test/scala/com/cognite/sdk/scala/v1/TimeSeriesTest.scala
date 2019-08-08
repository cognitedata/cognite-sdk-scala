package com.cognite.sdk.scala.v1

import java.time.Instant

import com.cognite.sdk.scala.common.{ReadBehaviours, SdkTest, WritableBehaviors}

class TimeSeriesTest extends SdkTest with ReadBehaviours with WritableBehaviors {
  private val idsThatDoNotExist = Seq(999991L, 999992L)
  private val externalIdsThatDoNotExist = Seq("5PNii0w4GCDBvXPZ", "6VhKQqtTJqBHGulw")

  it should behave like readable(client.timeSeries)

  it should behave like readableWithRetrieve(client.timeSeries, idsThatDoNotExist, supportsMissingAndThrown = true)

  it should behave like readableWithRetrieveByExternalId(client.timeSeries, externalIdsThatDoNotExist, supportsMissingAndThrown = true)

  it should behave like writable(
    client.timeSeries,
    Seq(
      TimeSeries(name = "scala-sdk-write-example-1"),
      TimeSeries(name = "scala-sdk-write-example-2")
    ),
    Seq(
      TimeSeriesCreate(name = "scala-sdk-create-example-1"),
      TimeSeriesCreate(name = "scala-sdk-create-example-2")
    ),
    idsThatDoNotExist,
    supportsMissingAndThrown = true
  )

  it should behave like writableWithExternalId(
    client.timeSeries,
    Seq(
      TimeSeries(name = "scala-sdk-write-external-example-1", externalId = Some(shortRandom())),
      TimeSeries(name = "scala-sdk-write-external-example-2", externalId = Some(shortRandom()))
    ),
    Seq(
      TimeSeriesCreate(name = "scala-sdk-create-external-example-1", externalId = Some(shortRandom())),
      TimeSeriesCreate(name = "scala-sdk-create-external-example-2", externalId = Some(shortRandom()))
    ),
    externalIdsThatDoNotExist,
    supportsMissingAndThrown = true
  )

  private val timeSeriesToCreate = Seq(
    TimeSeries(name = "scala-sdk-write-example-1", description = Some("description-1")),
    TimeSeries(name = "scala-sdk-write-example-2")
  )
  private val timeSeriesUpdates = Seq(
    TimeSeries(name = "scala-sdk-write-example-1-1", description = Some(null)), // scalastyle:ignore null
    TimeSeries(name = "scala-sdk-write-example-2-1", description = Some("scala-sdk-write-example-2"))
  )
  it should behave like updatable(
    client.timeSeries,
    timeSeriesToCreate,
    timeSeriesUpdates,
    (id: Long, item: TimeSeries) => item.copy(id = id),
    (a: TimeSeries, b: TimeSeries) => { a.copy(lastUpdatedTime = Instant.ofEpochMilli(0)) == b.copy(lastUpdatedTime = Instant.ofEpochMilli(0)) },
    (readTimeSeries: Seq[TimeSeries], updatedTimeSeries: Seq[TimeSeries]) => {
      assert(readTimeSeries.size == timeSeriesUpdates.size)
      assert(readTimeSeries.size == timeSeriesToCreate.size)
      assert(updatedTimeSeries.size == timeSeriesUpdates.size)
      assert(updatedTimeSeries.zip(readTimeSeries).forall { case (updated, read) => updated.name == s"${read.name}-1" })
      assert(updatedTimeSeries.head.description.isEmpty)
      assert(updatedTimeSeries(1).description == timeSeriesUpdates(1).description)
      ()
    }
  )
  it should "support search" in {
    val createdTimeSearchResults = client.timeSeries
      .search(
        TimeSeriesQuery(
          filter = Some(TimeSeriesFilter(createdTime = Some(TimeRange(Instant.ofEpochMilli(0), Instant.ofEpochMilli(0)))))
        )
      )
    assert(createdTimeSearchResults.length == 22)
    val createdTimeSearchResults2 = client.timeSeries.search(
        TimeSeriesQuery(
          filter = Some(
            TimeSeriesFilter(createdTime = Some(
              TimeRange(Instant.ofEpochMilli(1535964900000L), Instant.ofEpochMilli(1549000000000L)))))
        )
      )
    assert(createdTimeSearchResults2.length == 37)

    val unitSearchResults = client.timeSeries.search(
        TimeSeriesQuery(
          filter = Some(
            TimeSeriesFilter(unit = Some("m"),
              createdTime = Some(TimeRange(Instant.ofEpochMilli(0), Instant.ofEpochMilli(1549638383707L))))
          )
        )
      )
    assert(unitSearchResults.length == 33)

    val nameSearchResults = client.timeSeries
      .search(
        TimeSeriesQuery(
          filter = Some(
            TimeSeriesFilter(unit = Some("m"),
              createdTime = Some(TimeRange(Instant.ofEpochMilli(0), Instant.ofEpochMilli(1549638383707L))))
          ),
          search = Some(TimeSeriesSearch(name = Some("W0405")))
        )
      )
    assert(nameSearchResults.length == 12)

    val descriptionSearchResults = client.timeSeries.search(
      TimeSeriesQuery(
        filter = Some(
          TimeSeriesFilter(
            createdTime = Some(
              TimeRange(Instant.ofEpochMilli(1553632871254L), Instant.ofEpochMilli(1553632871254L))
            )
          )
        ),
        search = Some(TimeSeriesSearch(description = Some("Skarv")))
      )
    )
    assert(descriptionSearchResults.length == 51)

    val limitDescriptionSearchResults = client.timeSeries.search(
      TimeSeriesQuery(
        limit = 5,
        filter = Some(
          TimeSeriesFilter(
            createdTime = Some(
              TimeRange(Instant.ofEpochMilli(1553632871254L), Instant.ofEpochMilli(1553632871254L))
            )
          )
        ),
        search = Some(TimeSeriesSearch(description = Some("Skarv")))
      )
    )
    assert(limitDescriptionSearchResults.length == 5)
  }
}
