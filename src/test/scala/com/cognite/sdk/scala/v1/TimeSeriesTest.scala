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

  it should "support search" in {
    val createdTimeSearchResults = client.timeSeries
      .search(
        TimeSeriesQuery(
          filter = Some(TimeSeriesFilter(createdTime = Some(TimeRange(0, 0))))
        )
      )
      .unsafeBody
    assert(createdTimeSearchResults.length == 22)
    val createdTimeSearchResults2 = client.timeSeries.search(
        TimeSeriesQuery(
          filter =
            Some(TimeSeriesFilter(createdTime = Some(TimeRange(1535964900000L, 1549000000000L))))
        )
      )
      .unsafeBody
    assert(createdTimeSearchResults2.length == 37)

    val unitSearchResults = client.timeSeries.search(
        TimeSeriesQuery(
          filter = Some(
            TimeSeriesFilter(unit = Some("m"), createdTime = Some(TimeRange(0L, 1549638383707L)))
          )
        )
      ).unsafeBody
    assert(unitSearchResults.length == 33)

    val nameSearchResults = client.timeSeries
      .search(
        TimeSeriesQuery(
          filter = Some(
            TimeSeriesFilter(unit = Some("m"), createdTime = Some(TimeRange(0L, 1549638383707L)))
          ),
          search = Some(TimeSeriesSearch(name = Some("W0405")))
        )
      )
      .unsafeBody
    assert(nameSearchResults.length == 12)

    val descriptionSearchResults = client.timeSeries.search(
        TimeSeriesQuery(
          filter = Some(
            TimeSeriesFilter(createdTime = Some(TimeRange(1553632871254L, 1553632871254L)))
          ),
          search = Some(TimeSeriesSearch(description = Some("Skarv")))
        )
      ).unsafeBody
    assert(descriptionSearchResults.length == 51)

    val limitDescriptionSearchResults = client.timeSeries.search(
      TimeSeriesQuery(
        limit = 5,
        filter = Some(
          TimeSeriesFilter(createdTime = Some(TimeRange(1553632871254L, 1553632871254L)))
        ),
        search = Some(TimeSeriesSearch(description = Some("Skarv")))
      )
    ).unsafeBody
    assert(limitDescriptionSearchResults.length == 5)
  }
}
