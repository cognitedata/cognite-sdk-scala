// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import com.cognite.sdk.scala.common.OAuth2.ClientCredentials
import com.cognite.sdk.scala.common._
import io.circe.syntax.EncoderOps
import sttp.model.Uri

import java.time.temporal.ChronoUnit
import java.time.Instant
import scala.util.control.NonFatal
import java.util.UUID

@SuppressWarnings(
  Array("org.wartremover.warts.NonUnitStatements")
)
class TransformationsTest extends CommonDataModelTestHelper with RetryWhile {

  private val client = new GenericClient[IO](
    "scala-sdk-test",
    "extractor-bluefield-testing",
    baseUrl,
    authProvider,
    None,
    None,
    None
  )

  def shortRandomUUID(): String = UUID.randomUUID().toString.substring(0, 8)

  it should "list transformations" in {
    val res = client.transformations.list(Some(5)).compile.toList.unsafeRunSync()
    res.size shouldBe 5

    val resAll = client.transformations.list().compile.toList.unsafeRunSync()
    resAll.nonEmpty shouldBe true
    resAll.flatMap(_.lastFinishedJob).map(_.error).nonEmpty shouldBe true
  }

  it should "create and delete transformations" in {
    val uniquePrefix = shortRandomUUID()
    val transformationsToCreate = (0 to 3).map { i =>
      TransformationCreate(
        s"$uniquePrefix-transformation-sdk-test-${i.toString}",
        Some(s"select ${i.toString}"),
        Some(GenericDataSource("events")),
        conflictMode = Some("upsert"),
        Some(false),
        sourceOidcCredentials = Some(credentials),
        destinationOidcCredentials = Some(credentials),
        externalId = s"$uniquePrefix-transformation-sdk-test-${i.toString}",
        ignoreNullFields = true,
        dataSetId = None
      )
    }
    val externalIds = transformationsToCreate.map(_.externalId)
    try {
      val resCreates =
        client.transformations.createItems(Items(transformationsToCreate)).unsafeRunSync()
      resCreates.size shouldBe transformationsToCreate.size
      resCreates.map(_.name) should contain theSameElementsAs transformationsToCreate.map(_.name)
      resCreates.map(_.query) should contain theSameElementsAs transformationsToCreate.flatMap(
        _.query
      )
      resCreates.map(_.destination) should contain theSameElementsAs transformationsToCreate
        .flatMap(
          _.destination
        )
      resCreates.map(_.externalId) should contain theSameElementsAs transformationsToCreate.map(
        _.externalId
      )

      val externalIds = transformationsToCreate.map(_.externalId)
      retryWithExpectedResult[Seq[TransformationRead]](
        {
          client.transformations.delete(externalIds.map(CogniteExternalId(_)), true).unsafeRunSync()
          client.transformations.retrieveByExternalIds(externalIds, true).unsafeRunSync()
        },
        t => t shouldBe empty
      )
    } finally
      try
        client.transformations.delete(externalIds.map(CogniteExternalId(_)), true).unsafeRunSync()
      catch {
        case NonFatal(_) => // ignore
      }
  }

  it should "retrieve transformations by ids or externalIds" in {
    val uniquePrefix = shortRandomUUID()
    val transformationsToCreate = (0 to 3).map { i =>
      TransformationCreate(
        s"$uniquePrefix-transformation-sdk-test-${i.toString}",
        Some(s"select ${i.toString}"),
        Some(GenericDataSource("events")),
        conflictMode = Some("upsert"),
        Some(false),
        sourceOidcCredentials = Some(credentials),
        destinationOidcCredentials = Some(credentials),
        externalId = s"$uniquePrefix-transformation-sdk-test-${i.toString}",
        ignoreNullFields = true,
        dataSetId = None
      )
    }
    val resCreated =
      client.transformations.createItems(Items(transformationsToCreate)).unsafeRunSync()
    val externalIds = transformationsToCreate.map(_.externalId)
    val internalIds = resCreated.map(_.id)

    try {
      val resByIds = client.transformations.retrieveByIds(internalIds, true).unsafeRunSync()
      resByIds.size shouldBe transformationsToCreate.size

      val resByExternalIds =
        client.transformations.retrieveByExternalIds(externalIds, true).unsafeRunSync()
      resByExternalIds.size shouldBe transformationsToCreate.size
    } finally
      try
        client.transformations.delete(externalIds.map(CogniteExternalId(_)), true).unsafeRunSync()
      catch {
        case NonFatal(_) => // ignore
      }
  }

  it should "filter transformations" in {
    val uniquePrefix = shortRandomUUID()
    val existedDataSetId = 216250735038513L
    val transformationsToCreate = (0 to 3).map { i =>
      TransformationCreate(
        s"$uniquePrefix-transformation-sdk-test-${i.toString}",
        Some(s"select abc ${i.toString}"),
        Some(GenericDataSource("events")),
        conflictMode = Some("upsert"),
        Some(false),
        sourceOidcCredentials = Some(credentials),
        destinationOidcCredentials = Some(credentials),
        externalId = s"$uniquePrefix-transformation-sdk-test-${i.toString}",
        ignoreNullFields = true,
        dataSetId = Some(existedDataSetId)
      )
    }
    val resCreates =
      client.transformations.createItems(Items(transformationsToCreate)).unsafeRunSync()
    val externalIds = transformationsToCreate.map(_.externalId)

    try {
      val resFilterName = client.transformations
        .filter(TransformationsFilter(nameRegex = Some(s"$uniquePrefix")))
        .compile
        .toList
        .unsafeRunSync()
      resFilterName.size shouldBe transformationsToCreate.size
      resCreates.map(_.name) should contain theSameElementsAs resFilterName.map(_.name)

      val resFilterQueryRegex = client.transformations
        .filter(TransformationsFilter(queryRegex = Some("select abc")))
        .compile
        .toList
        .unsafeRunSync()
      resFilterQueryRegex.size should be >= transformationsToCreate.size
      resFilterQueryRegex.map(_.query).forall(q => q.startsWith("select abc")) shouldBe true

      val resFilterUpsertEvents = client.transformations
        .filter(
          TransformationsFilter(destinationType = Some("events"), conflictMode = Some("upsert"))
        )
        .compile
        .toList
        .unsafeRunSync()
      resFilterUpsertEvents.size should be >= transformationsToCreate.size
      resFilterUpsertEvents
        .map(_.destination)
        .toSet shouldBe Set(GenericDataSource("events"))
      resFilterUpsertEvents.map(_.conflictMode).toSet shouldBe Set("upsert")

      val max1DaysAgo = TimeFilter(max = Some(Instant.now().minus(1, ChronoUnit.DAYS).toEpochMilli))
      val resFilterCreatedTime1DayAgo = client.transformations
        .filter(
          TransformationsFilter(
            nameRegex = Some(s"$uniquePrefix-transformation-sdk-test"),
            createdTime = Some(max1DaysAgo)
          )
        )
        .compile
        .toList
        .unsafeRunSync()
      resFilterCreatedTime1DayAgo shouldBe empty

      val min3MinsAgo =
        TimeFilter(min = Some(Instant.now().minus(3, ChronoUnit.MINUTES).toEpochMilli))
      val resFilterCreatedTime3MinsAgo = client.transformations
        .filter(
          TransformationsFilter(
            nameRegex = Some(s"$uniquePrefix-transformation-sdk-test"),
            createdTime = Some(min3MinsAgo)
          )
        )
        .compile
        .toList
        .unsafeRunSync()
      resFilterCreatedTime3MinsAgo.size shouldBe transformationsToCreate.size

      val resFilterDataSetId = client.transformations
        .filter(
          TransformationsFilter(
            nameRegex = Some(s"$uniquePrefix-transformation-sdk-test"),
            dataSetIds = Some(Seq(CogniteInternalId(216250735038513L)))
          )
        )
        .compile
        .toList
        .unsafeRunSync()
      resFilterDataSetId.size shouldBe transformationsToCreate.size
    } finally
      try
        client.transformations.delete(externalIds.map(CogniteExternalId(_)), true).unsafeRunSync()
      catch {
        case NonFatal(_) => // ignore
      }
  }

  import FlatOidcCredentials.credentialEncoder

  "ClientCredentials encoder" should "work with empty scopes and empty audience" in {
    val credential = ClientCredentials(
      Uri.unsafeParse("http://tokenUrl.com"),
      "gcp",
      "secret",
      List.empty[String],
      "project",
      None
    ).asJson
    credential.toString() shouldBe """{
                                     |  "clientId" : "gcp",
                                     |  "clientSecret" : "secret",
                                     |  "tokenUri" : "http://tokenUrl.com",
                                     |  "cdfProjectName" : "project"
                                     |}""".stripMargin
  }

  it should "work with non empty scopes and non empty audience" in {
    val credential = ClientCredentials(
      Uri.unsafeParse("http://tokenUrl.com"),
      "gcp",
      "secret",
      (1 to 3).map(i => s"scope-${i.toString}").toList,
      "project",
      Some("audience")
    ).asJson
    credential.toString() shouldBe """{
                                     |  "clientId" : "gcp",
                                     |  "clientSecret" : "secret",
                                     |  "tokenUri" : "http://tokenUrl.com",
                                     |  "cdfProjectName" : "project",
                                     |  "audience" : "audience",
                                     |  "scopes" : "scope-1 scope-2 scope-3"
                                     |}""".stripMargin
  }

  it should "run transformation and get job by id" in {
    val uniquePrefix = shortRandomUUID()

    val newTransformation = TransformationCreate(
      s"$uniquePrefix-transformation-sdk-test",
      Some(s"select 1"),
      Some(GenericDataSource("events")),
      conflictMode = Some("upsert"),
      Some(false),
      sourceOidcCredentials = Some(credentials),
      destinationOidcCredentials = Some(credentials),
      externalId = s"$uniquePrefix-transformation-sdk-test",
      ignoreNullFields = true,
      dataSetId = None
    )
    val transformationExternalId = newTransformation.externalId

    try {
      val resCreates =
        client.transformations.createItems(Items(Seq(newTransformation))).unsafeRunSync()
      resCreates.size shouldBe 1
      val newJob = client.transformations.run(transformationExternalId).unsafeRunSync()
      newJob.transformationExternalId shouldBe newTransformation.externalId
      val retrievedJobs = client.transformations.retrieveJobByIds(Seq(newJob.id)).unsafeRunSync()
      retrievedJobs.map(_.id) should contain(newJob.id)
    } finally
      try
        client.transformations
          .delete(Seq(CogniteExternalId(transformationExternalId)), true)
          .unsafeRunSync()
      catch {
        case NonFatal(_) => // ignore
      }
  }

}
