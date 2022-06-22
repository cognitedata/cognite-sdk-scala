// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1

import cats.Id
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import com.cognite.sdk.scala.common._

import java.time.temporal.ChronoUnit
import java.time.Instant
import scala.concurrent.duration.DurationInt
import scala.util.control.NonFatal
import java.util.UUID

@SuppressWarnings(
  Array(
    "org.wartremover.warts.NonUnitStatements",
    "org.wartremover.warts.AsInstanceOf",
    "org.wartremover.warts.OptionPartial"
  )
)
class TransformationsTest extends CommonDataModelTestHelper with RetryWhile {

  private val auth = OAuth2
    .ClientCredentialsProvider[IO](credentials)
    .unsafeRunTimed(1.second)
    .get
    .getAuth
    .unsafeRunSync()
    .asInstanceOf[OidcTokenAuth]

  private lazy val client = new GenericClient[Id](
    "scala-sdk-test",
    "extractor-bluefield-testing",
    "https://bluefield.cognitedata.com",
    BearerTokenAuth(auth.bearerToken),
    None,
    None
  )

  def shortRandomUUID(): String = UUID.randomUUID().toString.substring(0, 8)

  it should "list transformations" in {
    val res = client.transformations.list(Some(5)).compile.toList
    res.size shouldBe 5
  }

  it should "create and delete transformations" in {
    val transformationsToCreate = (0 to 3).map { i =>
      val uniquePrefix = shortRandomUUID()
      TransformationCreate(
        s"$uniquePrefix-transformation-sdk-test",
        Some(s"select ${i.toString}"),
        Some(GeneralDataSource("events")),
        conflictMode = Some("upsert"),
        Some(false),
        sourceOidcCredentials = Some(credentials),
        destinationOidcCredentials = Some(credentials),
        externalId = s"$uniquePrefix-transformation-sdk-test",
        ignoreNullFields = true,
        dataSetId = None
      )
    }
    val resCreates = client.transformations.createItems(Items(transformationsToCreate))
    resCreates.size shouldBe transformationsToCreate.size
    resCreates.map(_.name) should contain theSameElementsAs transformationsToCreate.map(_.name)
    resCreates.map(_.query) should contain theSameElementsAs transformationsToCreate.flatMap(
      _.query
    )
    resCreates.map(_.destination) should contain theSameElementsAs transformationsToCreate.flatMap(
      _.destination
    )
    resCreates.map(_.externalId) should contain theSameElementsAs transformationsToCreate.map(
      _.externalId
    )

    val externalIds = transformationsToCreate.map(_.externalId)
    retryWithExpectedResult[Seq[TransformationRead]](
      {
        client.transformations.delete(externalIds.map(CogniteExternalId(_)), true)
        client.transformations
          .retrieveByExternalIds(externalIds, true)
      },
      t => t shouldBe empty
    )
  }

  it should "retrieve transformations by ids or externalIds" in {
    val transformationsToCreate = (0 to 3).map { i =>
      val uniquePrefix = shortRandomUUID()
      TransformationCreate(
        s"$uniquePrefix-transformation-sdk-test",
        Some(s"select ${i.toString}"),
        Some(GeneralDataSource("events")),
        conflictMode = Some("upsert"),
        Some(false),
        sourceOidcCredentials = Some(credentials),
        destinationOidcCredentials = Some(credentials),
        externalId = s"$uniquePrefix-transformation-sdk-test",
        ignoreNullFields = true,
        dataSetId = None
      )
    }
    val resCreated = client.transformations.createItems(Items(transformationsToCreate))
    val externalIds = transformationsToCreate.map(_.externalId)
    val internalIds = resCreated.map(_.id)

    try {
      val resByIds = client.transformations.retrieveByIds(internalIds, true)
      resByIds.size shouldBe transformationsToCreate.size

      val resByExternalIds = client.transformations.retrieveByExternalIds(externalIds, true)
      resByExternalIds.size shouldBe transformationsToCreate.size
    } finally
      try
        client.transformations.delete(externalIds.map(CogniteExternalId(_)), true)
      catch {
        case NonFatal(_) => // ignore
      }
  }

  it should "filter transformations" in {
    val uniquePrefix = shortRandomUUID()
    val transformationsToCreate = (0 to 3).map { i =>
      TransformationCreate(
        s"$uniquePrefix-transformation-sdk-test-${i.toString}",
        Some(s"select abc ${i.toString}"),
        Some(GeneralDataSource("events")),
        conflictMode = Some("upsert"),
        Some(false),
        sourceOidcCredentials = Some(credentials),
        destinationOidcCredentials = Some(credentials),
        externalId = s"$uniquePrefix-transformation-sdk-test-${i.toString}",
        ignoreNullFields = true,
        dataSetId = Some(216250735038513L)
      )
    }
    val resCreates = client.transformations.createItems(Items(transformationsToCreate))
    val externalIds = transformationsToCreate.map(_.externalId)

    try {
      val resFilterName = client.transformations
        .filter(TransformationsFilter(nameRegex = Some(s"$uniquePrefix")))
        .compile
        .toList
      resFilterName.size shouldBe transformationsToCreate.size
      resCreates.map(_.name) should contain theSameElementsAs resFilterName.map(_.name)

      val resFilterQueryRegex = client.transformations
        .filter(TransformationsFilter(queryRegex = Some("select abc")))
        .compile
        .toList
      resFilterQueryRegex.size should be >= transformationsToCreate.size
      resFilterQueryRegex.map(_.query).forall(q => q.startsWith("select abc")) shouldBe true

      val resFilterUpsertEvents = client.transformations
        .filter(
          TransformationsFilter(destinationType = Some("events"), conflictMode = Some("upsert"))
        )
        .compile
        .toList
      resFilterUpsertEvents.size should be >= transformationsToCreate.size
      resFilterUpsertEvents
        .map(_.destination.asInstanceOf[GeneralDataSource].`type`)
        .toSet shouldBe Set("events")
      resFilterUpsertEvents.map(_.conflictMode).toSet shouldBe Set("upsert")

      val max1DaysAgo = TimeFilter(max = Some(Instant.now().minus(1, ChronoUnit.DAYS).toEpochMilli))
      val resFilterCreatedTime1DayAgo = client.transformations
        .filter(
          TransformationsFilter(
            nameRegex = Some("transformation-sdk"),
            createdTime = Some(max1DaysAgo)
          )
        )
        .compile
        .toList
      resFilterCreatedTime1DayAgo shouldBe empty

      val min3MinsAgo =
        TimeFilter(min = Some(Instant.now().minus(3, ChronoUnit.MINUTES).toEpochMilli))
      val resFilterCreatedTime3MinsAgo = client.transformations
        .filter(
          TransformationsFilter(
            nameRegex = Some("transformation-sdk"),
            createdTime = Some(min3MinsAgo)
          )
        )
        .compile
        .toList
      resFilterCreatedTime3MinsAgo.size shouldBe transformationsToCreate.size

      val resFilterDataSetId = client.transformations
        .filter(
          TransformationsFilter(
            nameRegex = Some("transformation-sdk"),
            dataSetIds = Some(Seq(CogniteInternalId(216250735038513L)))
          )
        )
        .compile
        .toList
      resFilterDataSetId.size shouldBe transformationsToCreate.size
    } finally
      try
        client.transformations.delete(externalIds.map(CogniteExternalId(_)), true)
      catch {
        case NonFatal(_) => // ignore
      }

  }
}
