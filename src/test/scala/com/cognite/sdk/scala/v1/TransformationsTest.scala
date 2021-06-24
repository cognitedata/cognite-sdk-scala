// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1

import com.cognite.sdk.scala.common._
import io.circe.Json
import io.circe.generic.auto._

@SuppressWarnings(Array("org.wartremover.warts.TraversableOps", "org.wartremover.warts.NonUnitStatements", "org.wartremover.warts.Null"))
class TransformationsTest extends SdkTestSpec with ReadBehaviours with WritableBehaviors with RetryWhile {
  private val idsThatDoNotExist = Seq(999991L, 999992L)
  private val externalIdsThatDoNotExist = Seq("5PNii0w4GCDBvXPZ", "6VhKQqtTJqBHGulw")

  it should behave like readable(greenfieldClient.transformations)

  it should behave like readableWithRetrieve(greenfieldClient.transformations, idsThatDoNotExist, supportsMissingAndThrown = true)

  it should behave like readableWithRetrieveByExternalId(greenfieldClient.transformations, externalIdsThatDoNotExist, supportsMissingAndThrown = true)

  it should behave like readableWithRetrieveUnknownIds(greenfieldClient.transformations)

  it should behave like writable(
    greenfieldClient.transformations,
    Some(greenfieldClient.transformations),
    Seq(TransformConfigRead(
      name = "scala-sdk-read-example-1",
      id = 0,
      query = "select 1",
      destination = Json.obj("type" -> Json.fromString("events")),
      conflictMode = "upsert")),

    Seq(TransformConfigCreate(name = "scala-sdk-create-example-1")),
    idsThatDoNotExist,
    supportsMissingAndThrown = true
  )

  it should behave like writableWithExternalId(
    greenfieldClient.transformations,
    Some(greenfieldClient.transformations),
    Seq(TransformConfigRead(
      name = "scala-sdk-read-example-2",
      id = 0,
      query = "select 1",
      destination = Json.obj("type" -> Json.fromString("events")),
      conflictMode = "upsert",
      externalId = Some(shortRandom()))),

    Seq(TransformConfigCreate(name = "scala-sdk-create-example-2", externalId = Some(shortRandom()))),
    externalIdsThatDoNotExist,
    supportsMissingAndThrown = true
  )

  it should behave like deletableWithIgnoreUnknownIds(
    greenfieldClient.transformations,
    Seq(
      TransformConfigRead(
        name = "scala-sdk-read-example-2",
        id = 0,
        query = "select 1",
        destination = Json.obj("type" -> Json.fromString("events")),
        conflictMode = "upsert",
        externalId = Some(shortRandom()))
    ),
    idsThatDoNotExist
  )

  private val transformsToCreate = Seq(
    TransformConfigRead(
      name = "scala-sdk-read-example-2",
      id = 0,
      query = "select 1",
      destination = Json.obj("type" -> Json.fromString("events")),
      conflictMode = "upsert",
      externalId = Some(shortRandom())),
    TransformConfigRead(
      name = "scala-sdk-read-example-2",
      id = 0,
      query = "select 1",
      destination = Json.obj("type" -> Json.fromString("events")),
      conflictMode = "upsert",
      externalId = Some(shortRandom()))
  )
  private val transformUpdates = Seq(
    TransformConfigRead(
      name = "scala-sdk-read-example-2-1",
      id = 0,
      query = "select 1",
      destination = Json.obj("type" -> Json.fromString("events")),
      conflictMode = "upsert",
      externalId = Some(shortRandom())), // scalastyle:ignore null
    TransformConfigRead(
      name = "scala-sdk-read-example-2-1",
      id = 0,
      query = "select 1",
      destination = Json.obj("type" -> Json.fromString("events")),
      conflictMode = "upsert",
      externalId = Some(shortRandom()))
  )
  it should behave like updatable(
    greenfieldClient.transformations,
    Some(greenfieldClient.transformations),
    transformsToCreate,
    transformUpdates,
    (id: Long, item: TransformConfigRead) => item.copy(id = id),
    (a: TransformConfigRead, b: TransformConfigRead) => {
      a === b
    },
    (read: Seq[TransformConfigRead], updated: Seq[TransformConfigRead]) => {
      assert(transformsToCreate.size == updated.size)
      assert(read.size == updated.size)
      assert(updated.size == transformUpdates.size)
      assert(updated.map(_.name) == read.map(read => s"${read.name}-1"))
      ()
    }
  )
















  case class RawAggregationResponse(average: Double)

  it should "query average" in {
    val response = greenfieldClient.transformations.queryOne[RawAggregationResponse](
      "select avg(` V1 vcross (m/s)`) as average from ORCA.VAN_net"
    )
    println(response)
    assert(response.average > -1)
    assert(response.average < 1)
  }

  case class AssetIdentifier(id: Long, externalId: Option[String], name: String)

  it should "query assets" in {
    val response = greenfieldClient.transformations.query[AssetIdentifier](
      """select externalId, id, name
           from _cdf.assets
           where dayofweek(lastUpdatedTime) = 6
       """
    ).results.items
    println(response)
    assert(response.nonEmpty)
  }

//  it should behave like updatableById(
//    client.assets,
//    Some(client.assets),
//    assetsToCreate,
//    Seq(
//      AssetUpdate(name = Some(SetValue("scala-sdk-update-1-1"))),
//      AssetUpdate(name = Some(SetValue("scala-sdk-update-2-1")), dataSetId = Some(SetNull()))
//    ),
//    (readAssets: Seq[Asset], updatedAssets: Seq[Asset]) => {
//      assert(assetsToCreate.size == assetUpdates.size)
//      assert(readAssets.size == assetsToCreate.size)
//      assert(updatedAssets.size == assetUpdates.size)
//      assert(updatedAssets.zip(readAssets).forall { case (updated, read) => updated.name === s"${read.name}-1" })
//      assert(readAssets)
//      assert(List(None, None) === dataSets)
//      ()
//    }
//  )
//
//  it should behave like updatableByExternalId(
//    client.assets,
//    Some(client.assets),
//    Seq(
//      Asset(name = "update-1", externalId = Some("update-1-externalId")),
//      Asset(name = "update-2", externalId = Some("update-2-externalId"))),
//    Map("update-1-externalId" -> AssetUpdate(name = Some(SetValue("update-1-1"))),
//      "update-2-externalId" -> AssetUpdate(name = Some(SetValue("update-2-1")))),
//    (readAssets: Seq[Asset], updatedAssets: Seq[Asset]) => {
//      assert(assetsToCreate.size == assetUpdates.size)
//      assert(readAssets.size == assetsToCreate.size)
//      assert(updatedAssets.size == assetUpdates.size)
//      assert(updatedAssets.zip(readAssets).forall { case (updated, read) =>  updated.name === s"${read.name}-1" })
//      assert(updatedAssets.zip(readAssets).forall { case (updated, read) => updated.externalId === read.externalId })
//      ()
//    }
//  )

}
