// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1

import com.cognite.sdk.scala.common.OAuth2.ClientCredentials
import com.cognite.sdk.scala.common._
import com.cognite.sdk.scala.sttp.VcrMode
import sttp.client3._

import scala.util.control.NonFatal

@SuppressWarnings(
  Array("org.wartremover.warts.NonUnitStatements")
)
class TransformationsTest extends SdkVcrTestSpec with RetryWhile {

  // The OIDC credentials are sent in request bodies. VcrBackend redacts clientId, clientSecret and
  // tokenUri there, so real credentials are used when recording and dummies during playback.
  private def credentials: ClientCredentials =
    vcrMode match {
      case VcrMode.Playback =>
        ClientCredentials(
          uri"https://vcr-test.invalid/oauth2/token",
          "vcr-client-id",
          "vcr-client-secret",
          List(s"$baseUrl/.default"),
          projectName,
          Some(baseUrl)
        )
      case _ =>
        val s = envVarSuffix
        val tokenUri = sys.env
          .get(s"TEST_TOKEN_URL$s")
          .orElse(
            sys.env
              .get(s"TEST_AAD_TENANT$s")
              .map(t => s"https://login.microsoftonline.com/$t/oauth2/v2.0/token")
          )
          .getOrElse(sys.error(s"TEST_TOKEN_URL$s or TEST_AAD_TENANT$s must be set"))
        ClientCredentials(
          uri"$tokenUri",
          sys.env.getOrElse(s"TEST_CLIENT_ID$s", sys.error(s"TEST_CLIENT_ID$s must be set")),
          sys.env.getOrElse(s"TEST_CLIENT_SECRET$s", sys.error(s"TEST_CLIENT_SECRET$s must be set")),
          List(s"$baseUrl/.default"),
          projectName,
          Some(baseUrl)
        )
    }

  // A fixed point in time in place of Instant.now() so request bodies match the cassette.
  private val fixedPast = 1700000000000L

  it should "list transformations" in {
    val res = client.transformations.list(Some(5)).compile.toList.unsafeRunSync()
    res.size shouldBe 5

    val resAll = client.transformations.list().compile.toList.unsafeRunSync()
    resAll.nonEmpty shouldBe true
    resAll.flatMap(_.lastFinishedJob).map(_.error).nonEmpty shouldBe true
  }

  it should "create and delete transformations" in {
    val uniquePrefix = shortRandom()
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
        t => t shouldBe empty,
        sleep = sleepUnlessPlayback(_: Long)
      )
    } finally
      try
        client.transformations.delete(externalIds.map(CogniteExternalId(_)), true).unsafeRunSync()
      catch {
        case NonFatal(_) => // ignore
      }
  }

  it should "retrieve transformations by ids or externalIds" in {
    val uniquePrefix = shortRandom()
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
    val uniquePrefix = shortRandom()
    val existedDataSetId = testDataSet.id
    val dataDomainExternalId = s"$uniquePrefix-data-domain"
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
        dataSetId = Some(existedDataSetId),
        dataDomainExternalId = Some(dataDomainExternalId)
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

      val maxInThePast = TimeFilter(max = Some(fixedPast))
      val resFilterCreatedTime1DayAgo = client.transformations
        .filter(
          TransformationsFilter(
            nameRegex = Some(s"$uniquePrefix-transformation-sdk-test"),
            createdTime = Some(maxInThePast)
          )
        )
        .compile
        .toList
        .unsafeRunSync()
      resFilterCreatedTime1DayAgo shouldBe empty

      val min3MinsAgo = TimeFilter(min = Some(fixedPast))
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
            dataSetIds = Some(Seq(CogniteInternalId(existedDataSetId)))
          )
        )
        .compile
        .toList
        .unsafeRunSync()
      resFilterDataSetId.size shouldBe transformationsToCreate.size

      val resFilterDataDomainExternalId = client.transformations
        .filter(
          TransformationsFilter(
            nameRegex = Some(s"$uniquePrefix-transformation-sdk-test"),
            dataDomainExternalIds = Some(Seq(dataDomainExternalId))
          )
        )
        .compile
        .toList
        .unsafeRunSync()
      resFilterDataDomainExternalId.size shouldBe transformationsToCreate.size
      resFilterDataDomainExternalId
        .map(_.dataDomainExternalId)
        .toSet shouldBe Set(Some(dataDomainExternalId))
    } finally
      try
        client.transformations.delete(externalIds.map(CogniteExternalId(_)), true).unsafeRunSync()
      catch {
        case NonFatal(_) => // ignore
      }
  }

  it should "run transformation and get job by id" in {
    val uniquePrefix = shortRandom()

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
