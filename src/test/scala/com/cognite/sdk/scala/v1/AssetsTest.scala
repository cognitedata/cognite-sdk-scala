// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1

import java.time.Instant
import java.util.UUID
import com.cognite.sdk.scala.common._
import fs2.Stream

import scala.util.control.NonFatal

@SuppressWarnings(
  Array(
    "org.wartremover.warts.TraversableOps",
    "org.wartremover.warts.NonUnitStatements",
    "org.wartremover.warts.Null",
    "org.wartremover.warts.IterableOps",
    "org.wartremover.warts.SizeIs"
  )
)
class AssetsTest extends SdkTestSpec with ReadBehaviours with WritableBehaviors with RetryWhile {
  private val idsThatDoNotExist = Seq(999991L, 999992L)
  private val externalIdsThatDoNotExist = Seq("5PNii0w4GCDBvXPZ", "6VhKQqtTJqBHGulw")

  it should behave like readable(client.assets)

  it should behave like partitionedReadable(client.assets)

  it should behave like readableWithRetrieve(client.assets, idsThatDoNotExist, supportsMissingAndThrown = true)

  it should behave like readableWithRetrieveByExternalId(client.assets, externalIdsThatDoNotExist, supportsMissingAndThrown = true)

  it should behave like readableWithRetrieveUnknownIds(client.dataSets)

  it should behave like writable(
    client.assets,
    Some(client.assets),
    Seq(Asset(name = "scala-sdk-read-example-1"), Asset(name = "scala-sdk-read-example-2")),
    Seq(AssetCreate(name = "scala-sdk-create-example-1"), AssetCreate(name = "scala-sdk-create-example-2")),
    idsThatDoNotExist,
    supportsMissingAndThrown = true
  )

  it should behave like writableWithExternalId(
    client.assets,
    Some(client.assets),
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

  it should "support deleting entire asset subtrees recursively by externalId" in {
    val key = shortRandom()
    val assetTree = Seq(
        AssetCreate(name = "root", externalId = Some(s"$key-recursive-root")),
        AssetCreate(name = "child", externalId = Some(s"$key-recursive-child"), parentExternalId = Some(s"$key-recursive-root")),
        AssetCreate(name = "grandchild", externalId = Some(s"$key-recursive-grandchild"), parentExternalId = Some(s"$key-recursive-child"))
    )
    val createdAssets = client.assets.create(assetTree).unsafeRunSync()
    try {
      retryWithExpectedResult[Seq[Asset]](
        client.assets.filter(AssetsFilter(externalIdPrefix = Some(s"$key-recursive"))).compile.toList.unsafeRunSync(),
        r => r should have size 3
      )

      client.assets.deleteRecursive(Seq(CogniteExternalId(s"$key-recursive-root")), true, true).unsafeRunSync()

      retryWithExpectedResult[Seq[Asset]](
        client.assets.filter(AssetsFilter(externalIdPrefix = Some(s"$key-recursive"))).compile.toList.unsafeRunSync(),
        r => r should have size 0
      )
    } finally {
      try {
        client.assets.delete(createdAssets.map(event => CogniteInternalId(event.id)), ignoreUnknownIds = true).unsafeRunSync()
      } catch {
        case NonFatal(_) => // ignore
      }
    }
  }

  it should "support deleting entire asset subtrees recursively by id" in {
    val key = shortRandom()
    val assetTree = Seq(
      AssetCreate(name = "root", externalId = Some(s"$key-recursive-root")),
      AssetCreate(name = "child", externalId = Some(s"$key-recursive-child"), parentExternalId = Some(s"$key-recursive-root")),
      AssetCreate(name = "grandchild", externalId = Some(s"$key-recursive-grandchild"), parentExternalId = Some(s"$key-recursive-child"))
    )
    val createdAssets = client.assets.create(assetTree).unsafeRunSync()

    try {
      retryWithExpectedResult[Seq[Asset]](
        client.assets.filter(AssetsFilter(externalIdPrefix = Some(s"$key-recursive"))).compile.toList.unsafeRunSync(),
        r => r should have size 3
      )

      client.assets.deleteRecursive(Seq(CogniteInternalId(createdAssets(0).id)), true, true).unsafeRunSync()

      retryWithExpectedResult[Seq[Asset]](
        client.assets.filter(AssetsFilter(externalIdPrefix = Some(s"$key-recursive"))).compile.toList.unsafeRunSync(),
        r => r should have size 0
      )
    } finally {
      try {
        client.assets.delete(createdAssets.map(event => CogniteInternalId(event.id)), ignoreUnknownIds = true).unsafeRunSync()
      } catch {
        case NonFatal(_) => // ignore
      }
    }
  }

  private def createAssets(externalIdPrefix: String) = {
    val keys = (1 to 4).map(_ => shortRandom())
    val assets = keys.map(k => AssetCreate(name = k, externalId =  Some(s"$externalIdPrefix-$k")))
    val createdItems = client.assets.create(assets).unsafeRunSync()

    retryWithExpectedResult[Seq[Asset]](
      client.assets.filter(AssetsFilter(externalIdPrefix = Some(s"$externalIdPrefix"))).compile.toList.unsafeRunSync(),
      r => r should have size 4
    )
    createdItems
  }

  it should "support deleting by CogniteIds" in {
    val externalIdPrefix = s"delete-${shortRandom()}"
    val createdItems = createAssets(externalIdPrefix)

    val (deleteByInternalIds, deleteByExternalIds) = createdItems.splitAt(createdItems.size/2)
    val internalIds: Seq[CogniteId] = deleteByInternalIds.map(_.id).map(CogniteInternalId.apply)
    val externalIds: Seq[CogniteId] = deleteByExternalIds.flatMap(_.externalId).map(CogniteExternalId.apply)

    val cogniteIds = (internalIds ++ externalIds)

    client.assets.delete(cogniteIds, true).unsafeRunSync()

    //make sure that assets are deletes
    retryWithExpectedResult[Seq[Asset]](
      client.assets.filter(AssetsFilter(externalIdPrefix = Some(externalIdPrefix))).compile.toList.unsafeRunSync(),
      r => r should have size 0
    )
  }

  it should "raise a conflict error if input of delete contains internalIdand externalId that represent the same row" in {
    val prefix = s"delete-cogniteId-${shortRandom()}"
    val createdItems = createAssets(prefix)

    val (deleteByInternalIds, deleteByExternalIds) = createdItems.splitAt(createdItems.size/2)
    val internalIds: Seq[CogniteId] = deleteByInternalIds.map(_.id).map(CogniteInternalId.apply)
    val externalIds: Seq[CogniteId] = deleteByExternalIds.flatMap(_.externalId).map(CogniteExternalId.apply)

    val conflictInternalIdId:Seq[CogniteId] = Seq(CogniteInternalId.apply(deleteByExternalIds.head.id))
    an[CdpApiException] shouldBe thrownBy {
      client.assets.delete(externalIds ++ conflictInternalIdId, true).unsafeRunSync()
    }

    val conflictExternalId:Seq[CogniteId] = Seq(CogniteExternalId.apply(deleteByInternalIds.last.externalId.getOrElse("")))
    an[CdpApiException] shouldBe thrownBy {
      client.assets.delete(internalIds ++ conflictExternalId, true).unsafeRunSync()
    }

    client.assets.delete(internalIds ++ externalIds, true).unsafeRunSync()

    //make sure that assets are deletes
    retryWithExpectedResult[Seq[Asset]](
      client.assets.filter(AssetsFilter(externalIdPrefix = Some(prefix))).compile.toList.unsafeRunSync(),
      r => r should have size 0
    )
  }

  private val assetsToCreate = Seq(
    Asset(name = "scala-sdk-update-1", description = Some("description-1")),
    Asset(name = "scala-sdk-update-2", description = Some("description-2"), dataSetId = Some(testDataSet.id))
  )
  private val assetUpdates = Seq(
    Asset(name = "scala-sdk-update-1-1", description = null, dataSetId = Some(testDataSet.id)),
    Asset(name = "scala-sdk-update-2-1")
  )
  it should behave like updatable(
    client.assets,
    Some(client.assets),
    assetsToCreate,
    assetUpdates,
    (id: Long, item: Asset) => item.copy(id = id),
    (a: Asset, b: Asset) => {
      a.copy(lastUpdatedTime = Instant.ofEpochMilli(0)) === b.copy(lastUpdatedTime = Instant.ofEpochMilli(0))
    },
    (readAssets: Seq[Asset], updatedAssets: Seq[Asset]) => {
      assert(assetsToCreate.size == assetUpdates.size)
      assert(readAssets.size == assetsToCreate.size)
      assert(updatedAssets.size == assetUpdates.size)
      assert(updatedAssets.zip(readAssets).forall { case (updated, read) =>  updated.name === s"${read.name}-1" })
      assert(updatedAssets(0).description.isEmpty)
      assert(updatedAssets(1).description === readAssets(1).description)
      val dataSets = updatedAssets.map(_.dataSetId)
      assert(List(Some(testDataSet.id), Some(testDataSet.id)) === dataSets)
      ()
    }
  )

  it should "update labels on assets" in {
    val externalId1 = UUID.randomUUID.toString
    val externalId2 = UUID.randomUUID.toString
    val externalId3 = UUID.randomUUID.toString

    // Create labels
    client.labels.createItems(
      Items(Seq(LabelCreate(externalId = externalId1, name = externalId1),
        LabelCreate(externalId = externalId2, name = externalId2),
        LabelCreate(externalId = externalId3, name = externalId3)
      ))).unsafeRunSync()

    // Create assets
    val assetToCreate = Seq(
      AssetCreate(externalId = Some(externalId1),
        name=externalId1, labels = Some(Seq(CogniteExternalId(externalId1))), metadata = Some(Map("test1" -> "test1"))),
      AssetCreate(externalId = Some(externalId2), name=externalId2)
    )
    client.assets.createItems(Items(assetToCreate)).unsafeRunSync()

    // Update assets to test updates with labels
    val updatedAssets: Seq[Asset] = client.assets.updateByExternalId(Map(
      // Add the label with externalId=externalId2 and remove the label with externalId=externalId1 on asset1
      // Also test metadata partial updates
      externalId1 -> AssetUpdate(metadata = Some(UpdateMap(add = Map("test2"->"test2"))),
        labels = Some(UpdateArray(add = Seq(CogniteExternalId(externalId2)),
          remove = Seq(CogniteExternalId(externalId1))))),
      // Set labels to label with externalId=externalId2 on asset2
      externalId2 -> AssetUpdate(metadata = Some(SetValue(set = Map("test2"->"test2"))),
        labels = Some(SetValue(Seq(CogniteExternalId(externalId2)))))
     )
    ).unsafeRunSync()
    assert(updatedAssets.head.labels.contains(Seq(CogniteExternalId(externalId2))))
    assert(updatedAssets(1).labels.contains(Seq(CogniteExternalId(externalId2))))
    updatedAssets.head.metadata.toList.head  should contain theSameElementsAs  Map("test1"->"test1", "test2"->"test2")
    assert(updatedAssets(1).metadata.contains(Map("test2"->"test2")))

    // Test that omitting properties on AddRemoveArr doesn't have any effect
    // Test with empty lists on add/remove, basically do nothing
    client.assets.updateOneByExternalId(externalId1, AssetUpdate(labels = Some(UpdateArray()))).unsafeRunSync()
    // Test with empty list on remove, basically add label with externalId3
    val updated = client.assets.updateOneByExternalId(externalId1,
      AssetUpdate(labels = Some(UpdateArray(add = Seq(CogniteExternalId(externalId3)))))).unsafeRunSync()
    updated.labels.toList.head should contain theSameElementsAs Seq(CogniteExternalId(externalId2),
      CogniteExternalId(externalId3))

    client.assets.deleteByExternalIds(Seq(externalId1, externalId2)).unsafeRunSync()
    client.labels.deleteByExternalIds(Seq(externalId1, externalId2, externalId3)).unsafeRunSync()
  }

  it should "update metadata on assets with empty map" in {
    val externalId1 = UUID.randomUUID.toString

    // Create asset with metadata
    val assetToCreate = Seq(
      AssetCreate(externalId = Some(externalId1),
        name=externalId1, metadata = Some(Map("test1" -> "test1")))
    )

    val createdItems = client.assets.createItems(Items(assetToCreate)).unsafeRunSync()
    createdItems.head.metadata shouldBe Some(Map("test1" -> "test1"))

    // Updating metadata with None should have no effect
    val updatedAssetsWithNone: Seq[Asset] = client.assets.updateByExternalId(Map(
      externalId1 -> AssetUpdate(name = Some(SetValue("ML :)")), metadata = None))
    ).unsafeRunSync()

    val updatedAssets: Seq[Asset] = client.assets.updateByExternalId(Map(
      externalId1 -> AssetUpdate(metadata = Some(SetValue(set = Map()))))
    ).unsafeRunSync()
    client.assets.deleteByExternalIds(Seq(externalId1)).unsafeRunSync()

    updatedAssetsWithNone.head.metadata shouldBe Some(Map("test1" -> "test1"))
    updatedAssets.head.metadata shouldBe Some(Map())
  }

  it should behave like updatableById(
    client.assets,
    Some(client.assets),
    assetsToCreate,
    Seq(
      AssetUpdate(name = Some(SetValue("scala-sdk-update-1-1"))),
      AssetUpdate(name = Some(SetValue("scala-sdk-update-2-1")), dataSetId = Some(SetNull()))
    ),
    (readAssets: Seq[Asset], updatedAssets: Seq[Asset]) => {
      assert(assetsToCreate.size == assetUpdates.size)
      assert(readAssets.size == assetsToCreate.size)
      assert(updatedAssets.size == assetUpdates.size)
      assert(updatedAssets.zip(readAssets).forall { case (updated, read) => updated.name === s"${read.name}-1" })
      val dataSets = updatedAssets.map(_.dataSetId)
      assert(List(None, None) === dataSets)
      ()
    }
  )

  private val update1ExternalId = s"update-1-externalId-${UUID.randomUUID.toString.substring(0, 8)}"
  private val update2ExternalId = s"update-2-externalId-${UUID.randomUUID.toString.substring(0, 8)}"

  it should behave like updatableByExternalId(
    client.assets,
    Some(client.assets),
    Seq(
      Asset(name = "update-1", externalId = Some(update1ExternalId)),
      Asset(name = "update-2", externalId = Some(update2ExternalId))),
    Map(update1ExternalId -> AssetUpdate(name = Some(SetValue("update-1-1"))),
      update2ExternalId -> AssetUpdate(name = Some(SetValue("update-2-1")))),
    (readAssets: Seq[Asset], updatedAssets: Seq[Asset]) => {
      assert(assetsToCreate.size == assetUpdates.size)
      assert(readAssets.size == assetsToCreate.size)
      assert(updatedAssets.size == assetUpdates.size)
      assert(updatedAssets.zip(readAssets).forall { case (updated, read) =>  updated.name === s"${read.name}-1" })
      assert(updatedAssets.zip(readAssets).forall { case (updated, read) => updated.externalId === read.externalId })
      ()
    }
  )

  it should "support filter" in {
    retryWithExpectedResult[Seq[Asset]](
      client.assets
        .filter(
          AssetsFilter(
            createdTime = Some(
              TimeRange(Some(Instant.ofEpochMilli(1560756441301L)), Some(Instant.ofEpochMilli(1560756445000L)))))
        )
        .compile
        .toList
        .unsafeRunSync(),
      r => r should have size 84
    )

    val createdTimeFilterResults = client.assets
      .filter(
        AssetsFilter(
          createdTime = Some(
            TimeRange(Some(Instant.ofEpochMilli(1560756441301L)), Some(Instant.ofEpochMilli(1560756445000L)))))
      )
      .compile
      .toList
      .unsafeRunSync()
    assert(createdTimeFilterResults.length == 84)

    val createdTimeFilterPartitionResults = client.assets
      .filter(
        AssetsFilter(
          createdTime = Some(
            TimeRange(Some(Instant.ofEpochMilli(1560756441301L)), Some(Instant.ofEpochMilli(1560756445000L)))))
      )
      .compile
      .toList
      .unsafeRunSync()
    assert(createdTimeFilterPartitionResults.length == 84)

    val createdTimeFilterResultsWithLimit = client.assets
      .filter(
        AssetsFilter(
          createdTime = Some(
            TimeRange(Some(Instant.ofEpochMilli(1560756441301L)), Some(Instant.ofEpochMilli(1560756445000L))))),
        Some(10)
      )
      .compile
      .toList
      .unsafeRunSync()
    assert(createdTimeFilterResultsWithLimit.length == 10)

    retryWithExpectedResult[Seq[Asset]](
      client.assets
      .filterPartitions(
        AssetsFilter(
          assetSubtreeIds = Some(Seq(CogniteInternalId(7127045760755934L)))
        ), 10
      )
      .fold(Stream.empty)(_ ++ _)
      .compile
      .toList
        .unsafeRunSync(),
      a => a should have size 1106
    )

    val assetSubtreeIdsFilterResult = client.assets
      .filter(
        AssetsFilter(
          assetSubtreeIds = Some(Seq(CogniteInternalId(3028597755787717L))))
      )
      .compile
      .toList
      .unsafeRunSync()
    assert(assetSubtreeIdsFilterResult.length == 5)
  }

  it should "support asset aggregates" in {
    retryWithExpectedResult[Seq[Asset]](
      client.assets
        .filter(AssetsFilter(
          assetSubtreeIds = Some(Seq(CogniteInternalId(7127045760755934L)))
        ), Some(5), Some(Seq("childCount")))
        .compile.toList.unsafeRunSync(),
      a => {
        val bool = a.map(_.aggregates.value.get("childCount").value).exists(_ > 0)
        bool shouldBe true
      }
    )
  }

  it should "support search" in {
    val createdTimeSearchResults = client.assets
      .search(
        AssetsQuery(
          filter = Some(
            AssetsFilter(
              createdTime = Some(
                TimeRange(
                  Some(Instant.ofEpochMilli(1560756441301L)),
                  Some(Instant.ofEpochMilli(1560756445000L))
                )
              )
            )
          )
        )
      )
      .unsafeRunSync()
    assert(createdTimeSearchResults.length == 84)
    val valveResults = client.assets
      .search(
        AssetsQuery(
          filter = Some(
            AssetsFilter(
              createdTime =
                Some(TimeRange(Some(Instant.ofEpochMilli(0)), Some(Instant.ofEpochMilli(1560756460294L)))),
              parentIds = Some(Seq(1790957171927257L, 2436611095973105L, 6078796607206585L))
            )
          ),
          search = Some(AssetsSearch(description = Some("VALVE")))
        )
      )
      .unsafeRunSync()
    assert(valveResults.length == 3)
    val esdvResults = client.assets
      .search(
        AssetsQuery(
          search = Some(AssetsSearch(name = Some("ESDV")))
        )
      )
      .unsafeRunSync()
    assert(esdvResults.length == 20)
    val esdvLimitResults = client.assets
      .search(
        AssetsQuery(
          limit = 10,
          search = Some(AssetsSearch(name = Some("ESDV")))
        )
      )
      .unsafeRunSync()
    assert(esdvLimitResults.length == 10)
    val testAssets = client.assets
      .search(
        AssetsQuery(
          limit = 10,
          filter = Some(AssetsFilter(parentExternalIds = Some(Seq("test-root"))))
        )
      )
      .unsafeRunSync()
    assert(testAssets.map(_.parentExternalId) === Seq(Some("test-root"), Some("test-root")))
  }

  it should "not be an error to request more assets than the API limit" in {
    val _ = client.assets.list(Some(100000000)).take(10).compile.drain.unsafeRunSync()
  }

  it should "support search with dataSetIds" in {
    val created = client.assets.createFromRead(assetsToCreate).unsafeRunSync()
    try {
      val createdTimes = created.map(_.createdTime)
      val foundItems = retryWithExpectedResult[Seq[Asset]](
        client.assets.search(AssetsQuery(Some(AssetsFilter(
          dataSetIds = Some(Seq(CogniteInternalId(testDataSet.id))),
          createdTime = Some(TimeRange(
            min = Some(createdTimes.min),
            max = Some(createdTimes.max)
          ))
        )))).unsafeRunSync(),
        a => a should not be empty
      )
      foundItems.map(_.dataSetId) should contain only Some(testDataSet.id)
      created.filter(_.dataSetId.isDefined).map(_.id) should contain theSameElementsAs foundItems.map(_.id)
    } finally {
      client.assets.deleteByIds(created.map(_.id)).unsafeRunSync()
    }
  }

}
