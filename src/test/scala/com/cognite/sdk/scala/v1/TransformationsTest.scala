// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1

import com.cognite.sdk.scala.common._
import io.circe.Json
import io.circe.generic.auto._

import java.time.Instant

@SuppressWarnings(Array("org.wartremover.warts.TraversableOps", "org.wartremover.warts.NonUnitStatements", "org.wartremover.warts.Null"))
class TransformationsTest extends SdkTestSpec with ReadBehaviours with WritableBehaviors with RetryWhile {
  private val idsThatDoNotExist = Seq(999991L, 999992L)
  private val externalIdsThatDoNotExist = Seq("5PNii0w4GCDBvXPZ", "6VhKQqtTJqBHGulw")

  it should behave like readable(greenfieldClient.transformations)

  it should behave like readableWithRetrieve(greenfieldClient.transformations, idsThatDoNotExist, supportsMissingAndThrown = true)

  it should behave like readableWithRetrieveByExternalId(greenfieldClient.transformations, externalIdsThatDoNotExist, supportsMissingAndThrown = true)

  it should behave like readableWithRetrieveUnknownIds(greenfieldClient.transformations, nonExistentId = 999992)

  it should behave like writable(
    greenfieldClient.transformations,
    Some(greenfieldClient.transformations),
    Seq(TransformationRead(
      name = "scala-sdk-read-example-1",
      id = 0,
      query = "select 1",
      destination = Json.obj("type" -> Json.fromString("events")),
      conflictMode = "upsert")),

    Seq(TransformationCreate(name = "scala-sdk-create-example-1")),
    idsThatDoNotExist,
    supportsMissingAndThrown = true
  )

  it should behave like writableWithExternalId(
    greenfieldClient.transformations,
    Some(greenfieldClient.transformations),
    Seq(TransformationRead(
      name = "scala-sdk-read-example-2",
      id = 0,
      query = "select 1",
      destination = Json.obj("type" -> Json.fromString("events")),
      conflictMode = "upsert",
      externalId = Some(shortRandom()))),

    Seq(TransformationCreate(name = "scala-sdk-create-example-2", externalId = Some(shortRandom()))),
    externalIdsThatDoNotExist,
    supportsMissingAndThrown = true
  )

  it should behave like deletableWithIgnoreUnknownIds(
    greenfieldClient.transformations,
    Seq(
      TransformationRead(
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
    TransformationRead(
      name = "scala-sdk-read-example-2",
      id = 0,
      query = "select 1",
      destination = Json.obj("type" -> Json.fromString("events")),
      conflictMode = "upsert",
      externalId = Some(shortRandom())),
    TransformationRead(
      name = "scala-sdk-read-example-2",
      id = 0,
      query = "select 1",
      destination = Json.obj("type" -> Json.fromString("events")),
      conflictMode = "upsert",
      externalId = Some(shortRandom()))
  )
  private val transformUpdates = Seq(
    TransformationRead(
      name = "scala-sdk-read-example-2-1",
      id = 0,
      query = "select 1",
      destination = Json.obj("type" -> Json.fromString("events")),
      conflictMode = "upsert",
      externalId = Some(shortRandom())), // scalastyle:ignore null
    TransformationRead(
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
    (id: Long, item: TransformationRead) => item.copy(id = id),
    (a: TransformationRead, b: TransformationRead) => {
      a === b
    },
    (read: Seq[TransformationRead], updated: Seq[TransformationRead]) => {
      transformsToCreate.size shouldBe updated.size
      read.size shouldBe updated.size
      updated.size shouldBe transformUpdates.size
      updated.map(_.name) shouldBe read.map(read => s"${read.name}-1")
      ()
    }
  )

  it should behave like updatableByBothIds(
    greenfieldClient.transformations,
    Some(greenfieldClient.transformations),
    transformsToCreate,
    Seq(
      TransformationUpdate(name = Some(SetValue("scala-sdk-update-1-1"))),
      TransformationUpdate(
        destination = Some(SetValue(Json.obj("type" -> Json.fromString("datapoints")))),
        query = Some(SetValue("select 2")),
        sourceApiKey = Some(SetValue(greenfieldAuth.asInstanceOf[ApiKeyAuth].apiKey)),
        isPublic = Some(SetValue(true))
      )
    ),
    (read: Seq[TransformationRead], updated: Seq[TransformationRead]) => {
      assert(read.size == updated.size)
      assert(read.size == transformsToCreate.size)
      assert(read.size == transformUpdates.size)
      updated.map(_.name) shouldBe List("scala-sdk-update-1-1", "scala-sdk-read-example-2")
      assert(updated(1).isPublic)
      assert(!read(1).isPublic)
      assert(updated(1).hasSourceApiKey)
      updated(1).query shouldBe "select 2"
      ()
    }
  )

  case class RawAggregationResponse(average: Double)

  it should "query average" in {
    greenfieldClient.transformations.list().compile.toList
    val response = greenfieldClient.transformations.queryOne[RawAggregationResponse](
      "select avg(` V1 vcross (m/s)`) as average from ORCA.VAN_net"
    )
    assert(response.average > -1)
    assert(response.average < 1)
  }

  case class AssetIdentifier(id: Long, externalId: Option[String], name: String)

  it should "query assets" in {
    val assetId = "scalasdk-transforms-" + shortRandom()
    greenfieldClient.assets.createOne(AssetCreate(
      name = assetId,
      externalId = Some(assetId),
      description = Some("Test asset for transformations")
    ))

    val response =
      retryWithExpectedResult[Seq[AssetIdentifier]](
        greenfieldClient.transformations.query[AssetIdentifier](
          s"""select externalId, id, name
              from _cdf.assets
              where name = "$assetId"
           """,
          sourceLimit = None
        ).results.items,
        response => assert(response.length == 1)
      )
    response.head.externalId shouldBe Some(assetId)
    response.head.name shouldBe assetId
  }
}
@SuppressWarnings(Array("org.wartremover.warts.TraversableOps", "org.wartremover.warts.NonUnitStatements", "org.wartremover.warts.Null"))
class TransformationSchedulesTest extends SdkTestSpec with ReadBehaviours with WritableBehaviors with RetryWhile {
  private val idsThatDoNotExist = Seq(999991L, 999992L)
  private val externalIdsThatDoNotExist = Seq("5PNii0w4GCDBvXPZ", "6VhKQqtTJqBHGulw")
  private val someTransformation =
    greenfieldClient.transformations.createOne(TransformationCreate(
      name = "scala-sdk-create-noschedule",
      externalId = Some("transforms-test1-" + shortRandom()),
      query = Some("select 1"),
      destination = Some(Json.obj("type" -> Json.fromString("events"))),
      destinationApiKey = Some(greenfieldAuth.asInstanceOf[ApiKeyAuth].apiKey),
      sourceApiKey = Some(greenfieldAuth.asInstanceOf[ApiKeyAuth].apiKey)
    ))
  private val someTransformationWithSchedule =
    greenfieldClient.transformations.createOne(TransformationCreate(
      name = "scala-sdk-create-schedule",
      externalId = Some("transforms-test2-" + shortRandom()),
      query = Some("select 1"),
      destination = Some(Json.obj("type" -> Json.fromString("events"))),
      destinationApiKey = Some(greenfieldAuth.asInstanceOf[ApiKeyAuth].apiKey),
      sourceApiKey = Some(greenfieldAuth.asInstanceOf[ApiKeyAuth].apiKey)
    ))
  private val anotherTransformationWithSchedule =
    greenfieldClient.transformations.createOne(TransformationCreate(
      name = "scala-sdk-create-schedule2",
      externalId = Some("transforms-test3-" + shortRandom()),
      query = Some("select 1"),
      destination = Some(Json.obj("type" -> Json.fromString("events"))),
      destinationApiKey = Some(greenfieldAuth.asInstanceOf[ApiKeyAuth].apiKey),
      sourceApiKey = Some(greenfieldAuth.asInstanceOf[ApiKeyAuth].apiKey)
    ))
  private val theSchedule = greenfieldClient.transformations.schedules.createOne(TransformationScheduleCreate(
    interval = "1 1 1 1 1",
    id = Some(someTransformationWithSchedule.id),
    isPaused = Some(true)
  ))
  private val anotherSchedule = greenfieldClient.transformations.schedules.createOne(TransformationScheduleCreate(
    interval = "1 1 1 1 1",
    externalId = anotherTransformationWithSchedule.externalId,
    isPaused = Some(true)
  ))

  it should behave like readable(greenfieldClient.transformations.schedules)

  it should behave like readableWithRetrieve(greenfieldClient.transformations.schedules, idsThatDoNotExist, supportsMissingAndThrown = true)

  it should behave like readableWithRetrieveByExternalId(greenfieldClient.transformations.schedules, externalIdsThatDoNotExist, supportsMissingAndThrown = true)

  it should behave like readableWithRetrieveUnknownIds(greenfieldClient.transformations.schedules, nonExistentId = 999992)

  it should behave like writable(
    greenfieldClient.transformations.schedules,
    Some(greenfieldClient.transformations.schedules),
    Seq(TransformationScheduleRead(
      id = someTransformation.id,
      externalId = None,
      createdTime = Instant.now(),
      interval = "1 1 1 1 1",
      isPaused = false)),

    Seq(TransformationScheduleCreate("1 1 1 1 1", id = Some(someTransformation.id))),
    idsThatDoNotExist,
    supportsMissingAndThrown = true
  )

  it should behave like writableWithExternalId(
    greenfieldClient.transformations,
    Some(greenfieldClient.transformations),
    Seq(TransformationRead(
      name = "scala-sdk-read-example-2",
      id = 0,
      query = "select 1",
      destination = Json.obj("type" -> Json.fromString("events")),
      conflictMode = "upsert",
      externalId = Some(shortRandom()))),

    Seq(TransformationCreate(name = "scala-sdk-create-example-2", externalId = Some(shortRandom()))),
    externalIdsThatDoNotExist,
    supportsMissingAndThrown = true
  )

  it should behave like deletableWithIgnoreUnknownIds(
    greenfieldClient.transformations,
    Seq(
      TransformationRead(
        name = "scala-sdk-read-example-2",
        id = 0,
        query = "select 1",
        destination = Json.obj("type" -> Json.fromString("events")),
        conflictMode = "upsert",
        externalId = Some(shortRandom()))
    ),
    idsThatDoNotExist
  )

  it should "delete schedule and transforms" in {
    greenfieldClient.transformations.schedules.deleteById(theSchedule.id)
    greenfieldClient.transformations.schedules.deleteByExternalId(anotherSchedule.externalId.get)
    greenfieldClient.transformations.deleteByIds(Seq(someTransformation.id, someTransformationWithSchedule.id))
  }
}
