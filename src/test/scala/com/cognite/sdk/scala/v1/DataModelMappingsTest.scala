// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1

import cats.effect.unsafe.implicits.global
import com.cognite.sdk.scala.common.{Items, RetryWhile, SdkException}

import java.util.UUID
import scala.collection.immutable.Seq

@SuppressWarnings(
  Array(
    "org.wartremover.warts.PublicInference",
    "org.wartremover.warts.NonUnitStatements"
  )
)
class DataModelMappingsTest extends CommonDataModelTestHelper with RetryWhile {
  private val dataPropName = DataModelProperty("text", Some(true))
  private val dataPropDescription = DataModelProperty("text", Some(true))

  private val dataModel1 = DataModel(
    s"Equipment-${UUID.randomUUID.toString.substring(0, 8)}",
    Some(
      Map(
        "name" -> dataPropName,
        "description" -> dataPropDescription
      )
    )
  )

  private val dataPropBool = DataModelProperty("boolean", Some(true))
  private val dataPropFloat = DataModelProperty("float64", Some(true))

  private val dataModel2 = DataModel(
    s"Equipment-${UUID.randomUUID.toString.substring(0, 8)}",
    Some(
      Map(
        "prop_bool" -> dataPropBool,
        "prop_float" -> dataPropFloat
      )
    ),
    Some(Seq(dataModel1.externalId))
  )

  private val dataModels = Seq(dataModel1, dataModel2)

  val expectedDataModelsOutputWithProps = dataModels.map { dm =>
    dm.copy(properties =
      dm.properties.map(x => x ++ Map("externalId" -> DataModelProperty("text", Some(false))))
    )
  }

  private def insertDataModels() = {
    val outputCreates =
      blueFieldClient.dataModels
        .createItems(Items[DataModel](Seq(dataModel1, dataModel2)))
        .unsafeRunSync()
        .toList
    (outputCreates.size >= 2) shouldBe true
    outputCreates
  }

  private def deleteDataModels() = {
    blueFieldClient.dataModels
      .deleteItems(Seq(dataModel1.externalId, dataModel2.externalId))
      .unsafeRunSync()

    val outputList = blueFieldClient.dataModels.list(true).unsafeRunSync().toList
    expectedDataModelsOutputWithProps.foreach(dm => outputList.contains(dm) shouldBe false)
  }

  private def initAndCleanUpData(testCode: Seq[DataModel] => Any): Unit =
    try {
      val dataModelInstances: Seq[DataModel] = insertDataModels()
      val _ = testCode(dataModelInstances)
    } catch {
      case t: Throwable => throw t
    } finally {
      deleteDataModels()
      ()
    }

  "DataModelMappings" should "work for multiple externalIds" in initAndCleanUpData { _ =>
    val outputGetByIds =
      blueFieldClient.dataModelMappings
        .retrieveByExternalIds(
          Seq(dataModel1.externalId, dataModel2.externalId),
          false,
          false
        )
        .unsafeRunSync()
        .toList
    outputGetByIds.size shouldBe 2
  // VH TOTO Fix this when the api is updated
  // outputGetByIds.toSet shouldBe expectedDataModelsOutputWithProps.toSet
  }

  // VH TOTO Fix this when the api is updated
  ignore should "work with include inherited properties" in initAndCleanUpData { _ =>
    val expectedDataModel1 =
      dataModel1.copy(properties =
        dataModel1.properties.map(x =>
          x ++ Map("externalId" -> DataModelProperty("text", Some(false)))
        )
      )

    val expectedDataModel2 =
      dataModel2.copy(properties =
        dataModel2.properties.map(x =>
          x ++ Map("externalId" -> DataModelProperty("text", Some(false))) ++ dataModel1.properties
            .getOrElse(Map())
        )
      )

    val outputGetByIds =
      blueFieldClient.dataModelMappings
        .retrieveByExternalIds(
          Seq(dataModel1.externalId, dataModel2.externalId),
          includeInheritedProperties = true
        )
        .unsafeRunSync()
        .toList
    outputGetByIds.size shouldBe 2
    outputGetByIds.toSet shouldBe Set(expectedDataModel1, expectedDataModel2)
  }

  it should "work with ignore unknown externalIds" in initAndCleanUpData { _ =>
    val outputIgnoreUnknownIds =
      blueFieldClient.dataModelMappings
        .retrieveByExternalIds(
          Seq("toto"),
          ignoreUnknownIds = true
        )
        .unsafeRunSync()
        .toList
    outputIgnoreUnknownIds.isEmpty shouldBe true

    an[SdkException] should be thrownBy {
      blueFieldClient.dataModelMappings
        .retrieveByExternalIds(
          Seq("toto"),
          ignoreUnknownIds = false
        )
        .unsafeRunSync()
    }
  }

}
