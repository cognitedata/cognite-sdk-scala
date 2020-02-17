package com.cognite.sdk.scala.v1

import java.time.Instant
import com.cognite.sdk.scala.common._
import fs2.Stream

class TimeSeriesTest extends SdkTestSpec with ReadBehaviours with WritableBehaviors with RetryWhile {
  private val idsThatDoNotExist = Seq(999991L, 999992L, 999993L)
  private val externalIdsThatDoNotExist = Seq("5PNii0w4GCDBvXPZ", "6VhKQqtTJqBHGulw")

  it should behave like readable(client.timeSeries)

  it should behave like readableWithRetrieve(client.timeSeries, idsThatDoNotExist, supportsMissingAndThrown = true)

  it should behave like readableWithRetrieveByExternalId(client.timeSeries, externalIdsThatDoNotExist, supportsMissingAndThrown = true)

  it should behave like writable(
    client.timeSeries,
    Seq(
      TimeSeries(name = Some("scala-sdk-write-example-1")),
      TimeSeries(name = Some("scala-sdk-write-example-2")),
      TimeSeries()
    ),
    Seq(
      TimeSeriesCreate(name = Some("scala-sdk-create-example-1")),
      TimeSeriesCreate(name = Some("scala-sdk-create-example-2")),
      TimeSeriesCreate()
    ),
    idsThatDoNotExist,
    supportsMissingAndThrown = true
  )

  it should behave like writableWithExternalId(
    client.timeSeries,
    Seq(
      TimeSeries(name = Some("scala-sdk-write-external-example-1"), externalId = Some(shortRandom())),
      TimeSeries(name = Some("scala-sdk-write-external-example-2"), externalId = Some(shortRandom())),
      TimeSeries(externalId = Some(shortRandom()))
    ),
    Seq(
      TimeSeriesCreate(name = Some("scala-sdk-create-external-example-1"), externalId = Some(shortRandom())),
      TimeSeriesCreate(name = Some("scala-sdk-create-external-example-2"), externalId = Some(shortRandom())),
      TimeSeriesCreate(externalId = Some(shortRandom()))
    ),
    externalIdsThatDoNotExist,
    supportsMissingAndThrown = true
  )

  private val timeSeriesToCreate = Seq(
    TimeSeries(name = Some("scala-sdk-write-example-1"), description = Some("description-1"), dataSetId = Some(testDataSet.id)),
    TimeSeries(name = Some("scala-sdk-write-example-2"))
  )
  private val timeSeriesUpdates = Seq(
    TimeSeries(name = Some("scala-sdk-write-example-1-1"), description = Some(null)), // scalastyle:ignore null
    TimeSeries(name = Some("scala-sdk-write-example-2-1"), description = Some("scala-sdk-write-example-2"), dataSetId = Some(testDataSet.id))
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
      assert(updatedTimeSeries.zip(readTimeSeries).forall { case (updated, read) => updated.name == Some(s"${read.name.get}-1")})
      assert(updatedTimeSeries.head.description.isEmpty)
      assert(updatedTimeSeries(1).description == timeSeriesUpdates(1).description)
      val dataSets = updatedTimeSeries.map(_.dataSetId)
      assert(List(Some(testDataSet.id), Some(testDataSet.id)) === dataSets)
      ()
    }
  )

  it should behave like updatableById(
    client.timeSeries,
    timeSeriesToCreate,
    Seq(
      TimeSeriesUpdate(name = Some(SetValue("scala-sdk-write-example-1-1")), dataSetId = Some(SetNull())),
      TimeSeriesUpdate(name = Some(SetValue("scala-sdk-write-example-2-1")))
    ),
    (readTimeSeries: Seq[TimeSeries], updatedTimeSeries: Seq[TimeSeries]) => {
      assert(readTimeSeries.size == updatedTimeSeries.size)
      assert(updatedTimeSeries.zip(readTimeSeries).forall { case (updated, read) =>  updated.name.get == s"${read.name.get}-1" })
      val dataSets = updatedTimeSeries.map(_.dataSetId)
      assert(List(None, None) === dataSets)
      ()
    }
  )

  it should behave like updatableByExternalId(
    client.timeSeries,
    Seq(TimeSeries(name = Some("name-1"), externalId = Some("externalId-1")),
      TimeSeries(name = Some("name-2"), externalId = Some("externalId-2"))),
    Map("externalId-1" -> TimeSeriesUpdate(name = Some(SetValue("name-1-1"))),
      "externalId-2" -> TimeSeriesUpdate(name = Some(SetValue("name-2-1")))),
    (readTimeSeries: Seq[TimeSeries], updatedTimeSeries: Seq[TimeSeries]) => {
      assert(readTimeSeries.size == updatedTimeSeries.size)
      assert(updatedTimeSeries.zip(readTimeSeries).forall { case (updated, read) =>
        updated.name.getOrElse("") == s"${read.name.getOrElse("")}-1" })
      ()
    }
  )

  it should "support filter" in {
    val manyTimeSeriesForAsset = client.timeSeries
      .filter(
        TimeSeriesFilter(
          assetIds = Some(Seq(95453437348104L))
        )
      )
      .compile
      .toList
    manyTimeSeriesForAsset.size should be (327)

    val manyTimeSeriesForAssetWithLimit = client.timeSeries
      .filter(
        TimeSeriesFilter(
          assetIds = Some(Seq(95453437348104L))
        ),
        limit = Some(100)
      )
      .compile
      .toList
    manyTimeSeriesForAssetWithLimit.size should be (100)

    val fewTimeSeriesForOtherAsset = client.timeSeries
      .filter(
        TimeSeriesFilter(
          assetIds = Some(Seq(4480618819297421L))
        )
      )
      .compile
      .toList
    fewTimeSeriesForOtherAsset.size should be (1)

    val timeSeriesForBothAssets = client.timeSeries
      .filterPartitions(
        TimeSeriesFilter(
          assetIds = Some(Seq(4480618819297421L, 95453437348104L))
        ), 20
      )
      .fold(Stream.empty)(_ ++ _)
      .compile
      .toList
    timeSeriesForBothAssets.size should be (manyTimeSeriesForAsset.size + fewTimeSeriesForOtherAsset.size)

    val timeSeriesForUnknownAsset = client.timeSeries
      .filterPartitions(
        TimeSeriesFilter(
          assetIds = Some(Seq(44444L))
        ), 20
      )
      .fold(Stream.empty)(_ ++ _)
      .compile
      .toList
    timeSeriesForUnknownAsset.size should be (0)

    val timeSeriesForManyFilters = client.timeSeries
      .filterPartitions(
        TimeSeriesFilter(
          name = Some("VAL_23_FIC_92543_06:Z.X.Value"),
          isStep = Some(false),
          isString = Some(false),
          unit = None,
          assetIds = Some(Seq(95453437348104L)),
          externalIdPrefix = Some("VAL_23_FIC")
        ), 20, Some(5)
      )
      .fold(Stream.empty)(_ ++ _)
      .compile
      .toList
    timeSeriesForManyFilters.size should be (1)
  }

  it should "return the same results using filter as filterPartitions" in {
    val timeSeriesFilter = TimeSeriesFilter(
      isStep = Some(false),
      isString = Some(false),
      unit = None,
      externalIdPrefix = Some("VAL_23_FIC")
    )

    val tsFiltered = client.timeSeries
      .filter(timeSeriesFilter).compile.toList

    val tsFilteredByPartitions = client.timeSeries
        .filterPartitions(timeSeriesFilter, 8)
      .fold(Stream.empty)(_ ++ _)
      .compile.toList

    tsFiltered.size should be (10)
    assert(tsFiltered === tsFilteredByPartitions)
  }

  it should "support search" in {
    val createdTimeSearchResults = client.timeSeries
      .search(
        TimeSeriesQuery(
          filter = Some(
            TimeSeriesSearchFilter(
              createdTime = Some(TimeRange(Instant.ofEpochMilli(0), Instant.ofEpochMilli(0)))
            )
          )
        )
      )
    assert(createdTimeSearchResults.length == 20)
    val createdTimeSearchResults2 = client.timeSeries.search(
      TimeSeriesQuery(
        filter = Some(
          TimeSeriesSearchFilter(
            createdTime = Some(
              TimeRange(Instant.ofEpochMilli(1535964900000L), Instant.ofEpochMilli(1549000000000L))
            )
          )
        )
      )
    )
    assert(createdTimeSearchResults2.length == 2)

    val unitSearchResults = client.timeSeries.search(
      TimeSeriesQuery(
        filter = Some(
          TimeSeriesSearchFilter(
            unit = Some("m"),
            createdTime =
              Some(TimeRange(Instant.ofEpochMilli(0), Instant.ofEpochMilli(1549638383707L)))
          )
        )
      )
    )
    assert(unitSearchResults.length == 33)

    val nameSearchResults = client.timeSeries
      .search(
        TimeSeriesQuery(
          filter = Some(
            TimeSeriesSearchFilter(
              unit = Some("m"),
              createdTime =
                Some(TimeRange(Instant.ofEpochMilli(0), Instant.ofEpochMilli(1549638383707L)))
            )
          ),
          search = Some(TimeSeriesSearch(name = Some("W0405")))
        )
      )
    assert(nameSearchResults.length == 12)

    val descriptionSearchResults = client.timeSeries.search(
      TimeSeriesQuery(
        filter = Some(
          TimeSeriesSearchFilter(
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
          TimeSeriesSearchFilter(
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

  it should "be possible to create and query a time series without a name" in {
    val timeSeriesID = client.timeSeries.createFromRead(Seq(TimeSeries())).head.id
    val startTime = System.currentTimeMillis()
    val endTime = startTime + 20*1000
    val dp = (startTime to endTime by 1000).map(t =>
        DataPoint(Instant.ofEpochMilli(t), math.random))
    client.dataPoints.insertById(timeSeriesID, dp)
    val retrievedDp = client.dataPoints.queryById(
      timeSeriesID, Instant.ofEpochMilli(startTime), Instant.ofEpochMilli(endTime + 1000))
    retryWithExpectedResult[DataPointsByIdResponse](
      client.dataPoints.queryById(
        timeSeriesID, Instant.ofEpochMilli(startTime), Instant.ofEpochMilli(endTime + 1000)),
      Some(retrievedDp),
      Seq(dp => dp shouldBe retrievedDp)
    )
    client.dataPoints.deleteRangeById(timeSeriesID, Instant.ofEpochMilli(startTime), Instant.ofEpochMilli(endTime + 1000))
  }

  it should "support search with dataSetIds" in {
    val created = client.timeSeries.createFromRead(timeSeriesToCreate)
    try {
      val createdTimes = created.map(_.createdTime)
      val foundItems = retryWithExpectedResult(
        client.timeSeries.search(TimeSeriesQuery(Some(TimeSeriesSearchFilter(
          dataSetIds = Some(Seq(CogniteInternalId(testDataSet.id))),
          createdTime = Some(TimeRange(
            min = createdTimes.min,
            max = createdTimes.max
          ))
        )))),
        None,
        Seq((a: Seq[_]) => a should not be empty)
      )
      foundItems.map(_.dataSetId) should contain only Some(testDataSet.id)
      created.filter(_.dataSetId.isDefined).map(_.id) should contain only (foundItems.map(_.id): _*)
    } finally {
      client.timeSeries.deleteByIds(created.map(_.id))
    }
  }

}
