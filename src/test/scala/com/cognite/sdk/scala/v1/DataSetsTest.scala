package com.cognite.sdk.scala.v1
import java.time.Instant

import com.cognite.sdk.scala.common._
import com.cognite.sdk.scala.common.SetValue

class DataSetsTest extends SdkTestSpec {

  it should "able to read datasets" in {
    val listLength = client.dataSets.filter(DataSetFilter()).compile.toList.length

    val first1Length =
      client.dataSets.filter(DataSetFilter(), limit = Some(1)).compile.toList.length
    first1Length should be (Math.min(listLength, 1))
    val first2Length =
      client.dataSets.filter(DataSetFilter(), limit = Some(2)).compile.toList.length
    first2Length should be (Math.min(listLength, 2))
    val allLength = client.dataSets.filter(DataSetFilter(), limit = Some(3)).compile.toList.length
    allLength should be (Math.min(listLength, 3))
  }

  it should "able to create datasets" in {
    val random = shortRandom()
    //create single item
    val createdItem = client.dataSets.createOne(
      DataSetCreate(
        name = Some(s"testDataSet-name-${random}"),
        externalId = Some(s"testDataset1-externalId-${random}")))
    createdItem.id should not be 0
    assert(createdItem.externalId.contains(s"testDataset1-externalId-${random}"))

    //create multiple items
    val createdItems = client.dataSets.create(
      Seq(
        DataSetCreate(
          name = Some(s"testDataSet-name-1-${random}"),
          externalId = Some(s"testDataset1-externalId-1-${random}")),
        DataSetCreate(
          name = Some(s"testDataSet-name-2-${random}"),
          externalId = Some(s"testDataset1-externalId-2-${random}"))
      ))
    assert(createdItems.length == 2)
  }

  it should "support retrieving by ids and throw error for missing id" in {
    val firstTwoItemIds = client.dataSets.filter(DataSetFilter()).take(2).map(_.id).compile.toList
    assert(firstTwoItemIds.length == 2)

    val maybeItemsRead = client.dataSets.retrieveByIds(Seq(firstTwoItemIds(0),firstTwoItemIds(1)))
    val itemsReadIds = maybeItemsRead.map(_.id)

    assert(itemsReadIds.length == firstTwoItemIds.length)
    assert(itemsReadIds == firstTwoItemIds)

    assertThrows[CdpApiException] {
      client.dataSets.retrieveById(1234567890123L)
    }
  }

  it should "support retrieving by externalIds and throw error for missing ones" in {
    val maybeItemsRead = client.dataSets.retrieveByExternalId("testDataset1-externalId")

    assert(maybeItemsRead.externalId.contains("testDataset1-externalId"))

    assertThrows[CdpApiException] {
      client.dataSets.retrieveByExternalId("missing-id")
    }
  }

  it should "support update by id and externalId" in {
    val results =
      client.dataSets.filter(DataSetFilter(externalIdPrefix = Some("testDataset1-externalId"))).compile.toList

    val random = shortRandom()

    val updatedDatasetById = client.dataSets.updateById(
      Map(results.head.id -> DataSetUpdate(
        description = Some(SetValue(s"testDataset1-description-${random}")))))

    assert(updatedDatasetById.head.description.contains(s"testDataset1-description-${random}"))

    val updatedDatasetByExternalId = client.dataSets.updateByExternalId(
      Map("testDataset1-externalId" -> DataSetUpdate(
        metadata = Some(SetValue(Map("a" -> s"b-${random}"))))))

    assert(updatedDatasetByExternalId.head.metadata.get("a").equals(Some(s"b-${random}")))
  }

  it should "support filter" in {
    val createdTimeFilterResults = client.dataSets
      .filter(
        DataSetFilter(
          createdTime = Some(
            TimeRange(Instant.ofEpochMilli(1584403200000L), Instant.ofEpochMilli(1584525600000L))
          )
        )
      )
      .compile
      .toList
    assert(createdTimeFilterResults.length == 4)

    val createdTimeFilterResultsWithLimit = client.dataSets
      .filter(
        DataSetFilter(
          createdTime = Some(
            TimeRange(Instant.ofEpochMilli(1584403200000L), Instant.ofEpochMilli(1584525600000L))
          ),
          externalIdPrefix = Some("test")
        ),
        limit = Some(1)
      )
      .compile
      .toList

    assert(createdTimeFilterResultsWithLimit.length == 1)
  }
}
