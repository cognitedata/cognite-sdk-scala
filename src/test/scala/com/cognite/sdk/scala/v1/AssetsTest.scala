package com.cognite.sdk.scala.v1

import java.time.Instant

import com.cognite.sdk.scala.common.{ReadBehaviours, SdkTest, SetValue, WritableBehaviors}
import fs2.Stream

class AssetsTest extends SdkTest with ReadBehaviours with WritableBehaviors {
  private val idsThatDoNotExist = Seq(999991L, 999992L)
  private val externalIdsThatDoNotExist = Seq("5PNii0w4GCDBvXPZ", "6VhKQqtTJqBHGulw")

  it should behave like readable(client.assets)

  it should behave like partitionedReadable(client.assets)

  it should behave like readableWithRetrieve(client.assets, idsThatDoNotExist, supportsMissingAndThrown = true)

  it should behave like readableWithRetrieveByExternalId(client.assets, externalIdsThatDoNotExist, supportsMissingAndThrown = true)

  it should behave like writable(
    client.assets,
    Seq(Asset(name = "scala-sdk-read-example-1"), Asset(name = "scala-sdk-read-example-2")),
    Seq(AssetCreate(name = "scala-sdk-create-example-1"), AssetCreate(name = "scala-sdk-create-example-2")),
    idsThatDoNotExist,
    supportsMissingAndThrown = true
  )

  it should behave like writableWithExternalId(
    client.assets,
    Seq(
      Asset(name = "scala-sdk-read-example-1", externalId = Some(shortRandom())),
      Asset(name = "scala-sdk-read-example-2", externalId = Some(shortRandom()))
    ),
    Seq(
      AssetCreate(name = "scala-sdk-create-example-1", externalId = Some(shortRandom())),
      AssetCreate(name = "scala-sdk-create-example-2", externalId = Some(shortRandom()))
    ),
    externalIdsThatDoNotExist,
    supportsMissingAndThrown = true
  )

  it should behave like deletableWithIgnoreUnknownIds(
    client.assets,
    Seq(
      Asset(name = "scala-sdk-read-example-1", externalId = Some(shortRandom())),
      Asset(name = "scala-sdk-read-example-2", externalId = Some(shortRandom()))
    ),
    idsThatDoNotExist
  )

  private val assetsToCreate = Seq(
    Asset(name = "scala-sdk-update-1", description = Some("description-1")),
    Asset(name = "scala-sdk-update-2", description = Some("description-2"))
  )
  private val assetUpdates = Seq(
    Asset(name = "scala-sdk-update-1-1", description = null), // scalastyle:ignore null
    Asset(name = "scala-sdk-update-2-1")
  )
  it should behave like updatable(
    client.assets,
    assetsToCreate,
    assetUpdates,
    (id: Long, item: Asset) => item.copy(id = id),
    (a: Asset, b: Asset) => {
      a.copy(lastUpdatedTime = Instant.ofEpochMilli(0)) == b.copy(lastUpdatedTime = Instant.ofEpochMilli(0))
    },
    (readAssets: Seq[Asset], updatedAssets: Seq[Asset]) => {
      assert(assetsToCreate.size == assetUpdates.size)
      assert(readAssets.size == assetsToCreate.size)
      assert(updatedAssets.size == assetUpdates.size)
      assert(updatedAssets.zip(readAssets).forall { case (updated, read) =>  updated.name == s"${read.name}-1" })
      assert(updatedAssets.head.description.isEmpty)
      assert(updatedAssets(1).description == readAssets(1).description)
      ()
    }
  )

  it should behave like updatableById(
    client.assets,
    assetsToCreate,
    Seq(AssetUpdate(name = Some(SetValue("scala-sdk-update-1-1"))), AssetUpdate(name = Some(SetValue("scala-sdk-update-2-1")))),
    (readAssets: Seq[Asset], updatedAssets: Seq[Asset]) => {
      assert(assetsToCreate.size == assetUpdates.size)
      assert(readAssets.size == assetsToCreate.size)
      assert(updatedAssets.size == assetUpdates.size)
      assert(updatedAssets.zip(readAssets).forall { case (updated, read) =>  updated.name == s"${read.name}-1" })
      ()
    }
  )

  it should behave like updatableByExternalId(
    client.assets,
    Seq(
      Asset(name = "update-1", externalId = Some("update-1-externalId")),
      Asset(name = "update-2", externalId = Some("update-2-externalId"))),
    Map("update-1-externalId" -> AssetUpdate(name = Some(SetValue("update-1-1"))),
      "update-2-externalId" -> AssetUpdate(name = Some(SetValue("update-2-1")))),
    (readAssets: Seq[Asset], updatedAssets: Seq[Asset]) => {
      assert(assetsToCreate.size == assetUpdates.size)
      assert(readAssets.size == assetsToCreate.size)
      assert(updatedAssets.size == assetUpdates.size)
      assert(updatedAssets.zip(readAssets).forall { case (updated, read) =>  updated.name == s"${read.name}-1" })
      assert(updatedAssets.zip(readAssets).forall { case (updated, read) => updated.externalId == read.externalId })
      ()
    }
  )

  it should "support filter" in {
    val createdTimeFilterResults = client.assets
      .filter(
        AssetsFilter(
          createdTime = Some(
            TimeRange(Instant.ofEpochMilli(1560756441301L), Instant.ofEpochMilli(1560756445000L))))
      )
      .compile
      .toList
    assert(createdTimeFilterResults.length == 84)

    val createdTimeFilterPartitionResults = client.assets
      .filter(
        AssetsFilter(
          createdTime = Some(
            TimeRange(Instant.ofEpochMilli(1560756441301L), Instant.ofEpochMilli(1560756445000L))))
      )
      .compile
      .toList
    assert(createdTimeFilterPartitionResults.length == 84)

    val createdTimeFilterResultsWithLimit = client.assets
      .filter(
        AssetsFilter(
          createdTime = Some(
            TimeRange(Instant.ofEpochMilli(1560756441301L), Instant.ofEpochMilli(1560756445000L)))),
        Some(10)
      )
      .compile
      .toList
    assert(createdTimeFilterResultsWithLimit.length == 10)

    val filterWithRootAssetsByInternalId = client.assets
      .filterPartitions(
        AssetsFilter(
          rootIds = Some(Seq(CogniteInternalId(2780934754068396L)))
        ), 10
      )
      .fold(Stream.empty)(_ ++ _)
      .compile
      .toList
    assert(filterWithRootAssetsByInternalId.length == 781)
  }

  it should "support asset aggregates" in {
    val filterWithAggregates = client.assets
      .filter(AssetsFilter(
        rootIds = Some(Seq(CogniteInternalId(2780934754068396L)))
      ), Some(5), Some(Seq("childCount")))
        .compile.toList
    assert(filterWithAggregates.map(_.aggregates.get("childCount")).exists(_ > 0))
  }

  it should "support search" in {
    val createdTimeSearchResults = client.assets
      .search(
        AssetsQuery(
          filter = Some(
            AssetsFilter(
              createdTime = Some(
                TimeRange(
                  Instant.ofEpochMilli(1560756441301L),
                  Instant.ofEpochMilli(1560756445000L)
                )
              )
            )
          )
        )
      )
    assert(createdTimeSearchResults.length == 84)
    val valveResults = client.assets
      .search(
        AssetsQuery(
          filter = Some(
            AssetsFilter(
              createdTime =
                Some(TimeRange(Instant.ofEpochMilli(0), Instant.ofEpochMilli(1560756460294L))),
              parentIds = Some(Seq(1790957171927257L, 2436611095973105L, 6078796607206585L))
            )
          ),
          search = Some(AssetsSearch(description = Some("VALVE")))
        )
      )
    assert(valveResults.length == 3)
    val esdvResults = client.assets
      .search(
        AssetsQuery(
          search = Some(AssetsSearch(name = Some("ESDV")))
        )
      )
    assert(esdvResults.length == 15)
    val esdvLimitResults = client.assets
      .search(
        AssetsQuery(
          limit = 10,
          search = Some(AssetsSearch(name = Some("ESDV")))
        )
      )
    assert(esdvLimitResults.length == 10)
  }

  it should "not be an error to request more assets than the API limit" in {
    val _ = client.assets.list(Some(100000000)).take(10).compile.drain
  }

}
