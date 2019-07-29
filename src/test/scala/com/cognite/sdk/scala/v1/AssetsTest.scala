package com.cognite.sdk.scala.v1

import java.time.Instant

import cats.{Functor, Id}
import com.cognite.sdk.scala.common.{ReadBehaviours, SdkTest, WritableBehaviors}

class AssetsTest extends SdkTest with ReadBehaviours with WritableBehaviors {
  private val client = new GenericClient()(implicitly[Functor[Id]], auth, sttpBackend)
  private val idsThatDoNotExist = Seq(999991L, 999992L)
  private val externalIdsThatDoNotExist = Seq("5PNii0w4GCDBvXPZ", "6VhKQqtTJqBHGulw")

  it should behave like readable(client.assets)
  it should behave like readableWithRetrieve(client.assets, idsThatDoNotExist, supportsMissingAndThrown = true)
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
    (a: Asset, b: Asset) => { a == b },
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

    val createdTimeFilterResultsWithLimit = client.assets
      .filterWithLimit(
        AssetsFilter(
          createdTime = Some(
            TimeRange(Instant.ofEpochMilli(1560756441301L), Instant.ofEpochMilli(1560756445000L)))),
        10
      )
      .compile
      .toList
    assert(createdTimeFilterResultsWithLimit.length == 10)
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
}
