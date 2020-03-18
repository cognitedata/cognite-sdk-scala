package com.cognite.sdk.scala.v1
import java.time.Instant

import com.cognite.sdk.scala.common._
import com.cognite.sdk.scala.common.SetValue

class DataSetsTest extends SdkTestSpec {

  it should "able to create datasets if doesn't exists" in {
    val results =
      client.dataSet.filter(DataSetFilter(externalIdPrefix = Some("testDataset1-"))).compile.toList

    // We can't just keep on creating because datasets doesn't support deletes yet so we filter and then create.
    // We can have two datasets with same name but not same externalId.

    results.headOption.getOrElse({
      client.dataSet.create(
        Seq(
          DataSetCreate(
            name = Some("testDataSet-name"),
            externalId = Some("testDataset1-externalId"))))
    })
  }

  it should "support retrieving by ids and throw error for missing id" in {
    val firstTwoItemIds = client.dataSet.filter(DataSetFilter()).take(2).map(_.id).compile.to.seq
    assert(firstTwoItemIds.length == 2)

    val maybeItemsRead = client.dataSet.retrieveByIds(firstTwoItemIds)
    val itemsReadIds = maybeItemsRead.map(_.id)

    assert(itemsReadIds.length == firstTwoItemIds.length)
    assert(itemsReadIds == firstTwoItemIds)

    assertThrows[CdpApiException] {
      client.dataSet.retrieveById(1234567890123L)
    }
  }

  it should "support retrieving by externalIds and throw error for missing ones" in {
    val maybeItemsRead = client.dataSet.retrieveByExternalId("testDataset1-externalId")

    assert(maybeItemsRead.externalId == Some("testDataset1-externalId"))

    assertThrows[CdpApiException] {
      client.dataSet.retrieveByExternalId("missing-id")
    }
  }

  it should "support update by id and externalId" in {
    val results =
      client.dataSet.filter(DataSetFilter(externalIdPrefix = Some("testDataset1-"))).compile.toList
    val random = shortRandom()

    val updatedDatasetById = client.dataSet.updateById(
      Map(results.head.id -> DataSetUpdate(
        description = Some(SetValue(s"testDataset1-description-${random}")))))

    assert(updatedDatasetById.head.description.contains(s"testDataset1-description-${random}"))

    val updatedDatasetByExternalId = client.dataSet.updateByExternalId(
      Map("testDataset1-externalId" -> DataSetUpdate(
        metadata = Some(SetValue(Map("a" -> s"b-${random}"))))))

    assert(updatedDatasetByExternalId.head.metadata.get("a").equals(Some(s"b-${random}")))
  }

  it should "support filter" in {
    val createdTimeFilterResults = client.dataSet
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

    val createdTimeFilterResultsWithLimit = client.dataSet
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
