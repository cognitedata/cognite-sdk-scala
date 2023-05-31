// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1

import cats.Id

import java.time.Instant
import com.cognite.sdk.scala.common._
import fs2.Stream

@SuppressWarnings(
  Array(
    "org.wartremover.warts.TraversableOps",
    "org.wartremover.warts.NonUnitStatements",
    "org.wartremover.warts.Null",
    "org.wartremover.warts.IterableOps",
    "org.wartremover.warts.SizeIs"
  )
)
class TimeSeriesTest extends SdkTestSpec with ReadBehaviours with WritableBehaviors with RetryWhile {
  private val idsThatDoNotExist = Seq(999991L, 999992L, 999993L)
  private val externalIdsThatDoNotExist = Seq("5PNii0w4GCDBvXPZ", "6VhKQqtTJqBHGulw")

  it should behave like readable(client.timeSeries)

  it should behave like readableWithRetrieve(client.timeSeries, idsThatDoNotExist, supportsMissingAndThrown = true)

  it should behave like readableWithRetrieveByExternalId(client.timeSeries, externalIdsThatDoNotExist, supportsMissingAndThrown = true)

  it should behave like readableWithRetrieveUnknownIds(client.dataSets)

  it should behave like writable(
    client.timeSeries,
    Some(client.timeSeries),
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
    Some(client.timeSeries),
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
    Some(client.timeSeries),
    timeSeriesToCreate,
    timeSeriesUpdates,
    (id: Long, item: TimeSeries) => item.copy(id = id),
    (a: TimeSeries, b: TimeSeries) => { a.copy(lastUpdatedTime = Instant.ofEpochMilli(0)) === b.copy(lastUpdatedTime = Instant.ofEpochMilli(0)) },
    (readTimeSeries: Seq[TimeSeries], updatedTimeSeries: Seq[TimeSeries]) => {
      assert(readTimeSeries.size == timeSeriesUpdates.size)
      assert(readTimeSeries.size == timeSeriesToCreate.size)
      assert(updatedTimeSeries.size == timeSeriesUpdates.size)
      assert(updatedTimeSeries.zip(readTimeSeries).forall { case (updated, read) =>
        updated.name === Some(s"${read.name.value}-1")
      })
      assert(updatedTimeSeries.head.description.isEmpty)
      assert(updatedTimeSeries(1).description === timeSeriesUpdates(1).description)
      val dataSets = updatedTimeSeries.map(_.dataSetId)
      assert(List(Some(testDataSet.id), Some(testDataSet.id)) === dataSets)
      ()
    }
  )

  it should behave like updatableById(
    client.timeSeries,
    Some(client.timeSeries),
    timeSeriesToCreate,
    Seq(
      TimeSeriesUpdate(name = Some(SetValue("scala-sdk-write-example-1-1")), dataSetId = Some(SetNull())),
      TimeSeriesUpdate(name = Some(SetValue("scala-sdk-write-example-2-1")))
    ),
    (readTimeSeries: Seq[TimeSeries], updatedTimeSeries: Seq[TimeSeries]) => {
      assert(readTimeSeries.size == updatedTimeSeries.size)
      assert(updatedTimeSeries.zip(readTimeSeries).forall { case (updated, read) =>
        updated.name.value === s"${read.name.value}-1"
      })
      val dataSets = updatedTimeSeries.map(_.dataSetId)
      assert(List(None, None) === dataSets)
      ()
    }
  )

  private val updateExternalId1 = s"externalId-1-${shortRandom()}"
  private val updateExternalId2 = s"externalId-1-${shortRandom()}"

  it should behave like updatableByExternalId(
    client.timeSeries,
    Some(client.timeSeries),
    Seq(TimeSeries(name = Some("name-1"), externalId = Some(updateExternalId1)),
      TimeSeries(name = Some("name-2"), externalId = Some(updateExternalId2))),
    Map(updateExternalId1 -> TimeSeriesUpdate(name = Some(SetValue("name-1-1"))),
      updateExternalId2 -> TimeSeriesUpdate(name = Some(SetValue("name-2-1")))),
    (readTimeSeries: Seq[TimeSeries], updatedTimeSeries: Seq[TimeSeries]) => {
      assert(readTimeSeries.size == updatedTimeSeries.size)
      assert(updatedTimeSeries.zip(readTimeSeries).forall { case (updated, read) =>
        updated.name.getOrElse("") === s"${read.name.getOrElse("")}-1" })
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
      .unsafeRunSync()
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
      .unsafeRunSync()
    manyTimeSeriesForAssetWithLimit.size should be (100)

    val fewTimeSeriesForOtherAsset = client.timeSeries
      .filter(
        TimeSeriesFilter(
          assetIds = Some(Seq(4480618819297421L))
        )
      )
      .compile
      .toList
      .unsafeRunSync()
    fewTimeSeriesForOtherAsset.size should be (1)

    val timeSeriesForBothAssets = client.timeSeries
      .filterPartitions(
        TimeSeriesFilter(
          assetIds = Some(Seq(4480618819297421L, 95453437348104L))
        ), 10
      )
      .fold(Stream.empty)(_ ++ _)
      .compile
      .toList
      .unsafeRunSync()
    timeSeriesForBothAssets.size should be (manyTimeSeriesForAsset.size + fewTimeSeriesForOtherAsset.size)

    val timeSeriesForUnknownAsset = client.timeSeries
      .filterPartitions(
        TimeSeriesFilter(
          assetIds = Some(Seq(44444L))
        ), 10
      )
      .fold(Stream.empty)(_ ++ _)
      .compile
      .toList
      .unsafeRunSync()
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
        ), 10, Some(5)
      )
      .fold(Stream.empty)(_ ++ _)
      .compile
      .toList
      .unsafeRunSync()
    timeSeriesForManyFilters.size should be (1)
  }

  it should behave like deletableWithIgnoreUnknownIds(
    client.timeSeries,
    Seq(
      TimeSeries(name = Some("scala-sdk-delete-example-1"), externalId = Some(shortRandom())),
      TimeSeries(description = Some("scala-sdk-delete-example-2"), externalId = Some(shortRandom()))
    ),
    idsThatDoNotExist
  )

  private def createTimeSeries(externalIdPrefix: String) = {
    val keys = (1 to 4).map(_ => shortRandom())
    val timeseries = keys.map(k=>
      TimeSeriesCreate(name = Some("scala-sdk-delete-cogniteId-" + k), externalId = Some(s"$externalIdPrefix-$k"))
    )
    val createdItems = client.timeSeries.create(timeseries).unsafeRunSync()

    retryWithExpectedResult[Seq[TimeSeries]](
      client.timeSeries.filter(TimeSeriesFilter(externalIdPrefix = Some(s"$externalIdPrefix-"))).compile.toList.unsafeRunSync(),
      r => r should have size 4
    )
    createdItems
  }

  it should "support deleting by CogniteIds" in {
    val prefix = s"delete-cogniteId-${shortRandom()}"
    val createdItems = createTimeSeries(prefix)

    val (deleteByInternalIds, deleteByExternalIds) = createdItems.splitAt(createdItems.size/2)
    val internalIds: Seq[CogniteId] = deleteByInternalIds.map(_.id).map(CogniteInternalId.apply)
    val externalIds: Seq[CogniteId]  = deleteByExternalIds.flatMap(_.externalId).map(CogniteExternalId.apply)

    val cogniteIds = (internalIds ++ externalIds)

    client.timeSeries.delete(cogniteIds, true).unsafeRunSync()

    retryWithExpectedResult[Seq[TimeSeries]](
      client.timeSeries.filter(TimeSeriesFilter(externalIdPrefix = Some(prefix))).compile.toList.unsafeRunSync(),
      r => r should have size 0
    )
  }

  it should "raise a conflict error if input of delete contains internalId and externalId that represent the same row" in {
    val prefix = s"delete-cogniteId-${shortRandom()}"
    val createdItems = createTimeSeries(prefix)

    val (deleteByInternalIds, deleteByExternalIds) = createdItems.splitAt(createdItems.size/2)
    val internalIds: Seq[CogniteId] = deleteByInternalIds.map(_.id).map(CogniteInternalId.apply)
    val externalIds: Seq[CogniteId] = deleteByExternalIds.flatMap(_.externalId).map(CogniteExternalId.apply)

    val conflictInternalIdId:Seq[CogniteId] = Seq(CogniteInternalId.apply(deleteByExternalIds.head.id))
    an[CdpApiException] shouldBe thrownBy {
      client.timeSeries.delete(externalIds ++ conflictInternalIdId, true).unsafeRunSync()
    }

    val conflictExternalId:Seq[CogniteId] = Seq(CogniteExternalId.apply(deleteByInternalIds.last.externalId.getOrElse("")))
    an[CdpApiException] shouldBe thrownBy {
      client.timeSeries.delete(internalIds ++ conflictExternalId, true).unsafeRunSync()
    }

    client.timeSeries.delete(internalIds ++ externalIds, true).unsafeRunSync()

    //make sure that timeSeries are deletes
    retryWithExpectedResult[Seq[TimeSeries]](
      client.timeSeries.filter(TimeSeriesFilter(externalIdPrefix = Some(prefix))).compile.toList.unsafeRunSync(),
      r => r should have size 0
    )
  }

  it should "return the same results using filter as filterPartitions" in {
    val timeSeriesFilter = TimeSeriesFilter(
      isStep = Some(false),
      isString = Some(false),
      unit = None,
      externalIdPrefix = Some("VAL_23_FIC")
    )

    val tsFiltered = client.timeSeries
      .filter(timeSeriesFilter).compile.toList.unsafeRunSync()

    val tsFilteredByPartitions = client.timeSeries
        .filterPartitions(timeSeriesFilter, 8)
      .fold(Stream.empty)(_ ++ _)
      .compile.toList.unsafeRunSync()

    tsFiltered.size should be (10)
    assert(tsFiltered === tsFilteredByPartitions)
  }

  it should "support search" in {
    val createdTimeSearchResults = client.timeSeries
      .search(
        TimeSeriesQuery(
          filter = Some(
            TimeSeriesSearchFilter(
              createdTime = Some(TimeRange(Some(Instant.ofEpochMilli(0)), Some(Instant.ofEpochMilli(0))))
            )
          )
        )
      )
      .unsafeRunSync()
    assert(createdTimeSearchResults.length == 20)
    val createdTimeSearchResults2 = client.timeSeries.search(
      TimeSeriesQuery(
        filter = Some(
          TimeSeriesSearchFilter(
            createdTime = Some(
              TimeRange(Some(Instant.ofEpochMilli(1535964900000L)), Some(Instant.ofEpochMilli(1549000000000L)))
            )
          )
        )
      )
    ).unsafeRunSync()
    assert(createdTimeSearchResults2.length == 2)

    val unitSearchResults = client.timeSeries.search(
      TimeSeriesQuery(
        filter = Some(
          TimeSeriesSearchFilter(
            unit = Some("m"),
            createdTime =
              Some(TimeRange(Some(Instant.ofEpochMilli(0)), Some(Instant.ofEpochMilli(1549638383707L))))
          )
        )
      )
    ).unsafeRunSync()
    assert(unitSearchResults.length == 33)

    val nameSearchResults = client.timeSeries
      .search(
        TimeSeriesQuery(
          filter = Some(
            TimeSeriesSearchFilter(
              unit = Some("m"),
              createdTime =
                Some(TimeRange(Some(Instant.ofEpochMilli(0)), Some(Instant.ofEpochMilli(1549638383707L))))
            )
          ),
          search = Some(TimeSeriesSearch(name = Some("W0405")))
        )
      )
      .unsafeRunSync()
    assert(nameSearchResults.length == 19)

    val descriptionSearchResults = client.timeSeries.search(
      TimeSeriesQuery(
        filter = Some(
          TimeSeriesSearchFilter(
            createdTime = Some(
              TimeRange(Some(Instant.ofEpochMilli(1553632871254L)), Some(Instant.ofEpochMilli(1553632871254L)))
            )
          )
        ),
        search = Some(TimeSeriesSearch(description = Some("Skarv")))
      )
    ).unsafeRunSync()
    assert(descriptionSearchResults.length == 51)

    val limitDescriptionSearchResults = client.timeSeries.search(
      TimeSeriesQuery(
        limit = 5,
        filter = Some(
          TimeSeriesSearchFilter(
            createdTime = Some(
              TimeRange(Some(Instant.ofEpochMilli(1553632871254L)), Some(Instant.ofEpochMilli(1553632871254L)))
            )
          )
        ),
        search = Some(TimeSeriesSearch(description = Some("Skarv")))
      )
    ).unsafeRunSync()
    assert(limitDescriptionSearchResults.length == 5)
  }

  it should "be possible to create and query a time series without a name" in {
    val timeSeriesID = client.timeSeries.createFromRead(Seq(TimeSeries())).unsafeRunSync().head.id
    val startTime = System.currentTimeMillis()
    val endTime = startTime + 20*1000
    val dp = (startTime to endTime by 1000).map(t =>
        DataPoint(Instant.ofEpochMilli(t), java.lang.Math.random()))
    client.dataPoints.insert(CogniteInternalId(timeSeriesID), dp).unsafeRunSync()
    retryWithExpectedResult[DataPointsByIdResponse](
      client.dataPoints.queryById(
        timeSeriesID, Instant.ofEpochMilli(startTime), Instant.ofEpochMilli(endTime + 1000)).unsafeRunSync(),
      retrievedDp => retrievedDp.datapoints shouldBe dp)
    client.dataPoints.deleteRangeById(timeSeriesID, Instant.ofEpochMilli(startTime), Instant.ofEpochMilli(endTime + 1000)).unsafeRunSync()
  }

  it should "support search with dataSetIds" in {
    val created = client.timeSeries.createFromRead(timeSeriesToCreate).unsafeRunSync()
    try {
      val createdTimes = created.map(_.createdTime)
      val foundItems = retryWithExpectedResult(
        client.timeSeries.search(TimeSeriesQuery(Some(TimeSeriesSearchFilter(
          dataSetIds = Some(Seq(CogniteInternalId(testDataSet.id))),
          createdTime = Some(TimeRange(
            min = Some(createdTimes.min),
            max = Some(createdTimes.max)
          ))
        )))).unsafeRunSync(),
        (a: Seq[_]) => a should not be empty
      )
      foundItems.map(_.dataSetId) should contain only Some(testDataSet.id)
      created.filter(_.dataSetId.isDefined).map(_.id) should contain theSameElementsAs foundItems.map(_.id)
    } finally {
      client.timeSeries.deleteByIds(created.map(_.id)).unsafeRunSync()
    }
  }

  it should "support reading synthetic queries" in {
    val query = SyntheticTimeSeriesQuery("5 + TS{id=54577852743225}",
      Instant.ofEpochMilli(0),
      Instant.ofEpochMilli(1646906518178L),
      1000)
    val syntheticQuery: Id[Seq[SyntheticTimeSeriesResponse]] = client.timeSeries.syntheticQuery(Items(Seq(query))).unsafeRunSync()
    syntheticQuery.head.datapoints.length should be(1000)
  }
}
