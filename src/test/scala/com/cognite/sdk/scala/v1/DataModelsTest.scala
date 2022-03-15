// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1

import cats.effect.unsafe.implicits.global
import com.cognite.sdk.scala.common.SdkException
//import cats.Id
import com.cognite.sdk.scala.common.{Items, RetryWhile}
//import sttp.client3.testing.SttpBackendStub
//import sttp.model.{Header, MediaType, Method, StatusCode}

import java.util.UUID
import scala.collection.immutable.Seq

@SuppressWarnings(
  Array(
    "org.wartremover.warts.PublicInference",
    "org.wartremover.warts.NonUnitStatements"
  )
)
class DataModelsTest extends CommonDataModelTestHelper with RetryWhile {

  val uuid = UUID.randomUUID.toString
  val dataPropName = DataModelProperty("text", true)
  val dataPropDescription = DataModelProperty("text", true)
  // val dataPropIndex = DataModelPropertyIndex(Some("name_descr"), Some(Seq("name", "description")))

  val dataModel = DataModel(
    s"Equipment-${uuid.substring(0, 8)}",
    Some(
      Map(
        "name" -> dataPropName,
        "description" -> dataPropDescription
      )
    ),
    None, // Some(Seq("Asset", "Pump")),
    None // Some(Seq(dataPropIndex))
  )

  val expectedDataModelOutput = dataModel.copy(properties =
    dataModel.properties.map(x => x ++ Map("externalId" -> DataModelProperty("text", false)))
  )

  "DataModels" should "create data models definitions" in {
    val dataModels =
      blueFieldClient.dataModels
        .createItems(Items[DataModel](Seq(dataModel)))
        .unsafeRunSync()
        .toList
    dataModels.contains(dataModel) shouldBe true
  }

  it should "list all data models definitions" in {
    val dataModels = blueFieldClient.dataModels.list(true).unsafeRunSync().toList
    dataModels.nonEmpty shouldBe true
    dataModels.contains(expectedDataModelOutput) shouldBe true
  }

  it should "delete data models definitions" in {
    blueFieldClient.dataModels.deleteItems(Seq(dataModel.externalId)).unsafeRunSync()

    val dataModels = blueFieldClient.dataModels.list().unsafeRunSync().toList
    dataModels.contains(expectedDataModelOutput) shouldBe false
  }

  private val dataModel1 = DataModel(
    s"Equipment-${UUID.randomUUID.toString.substring(0, 8)}",
    Some(
      Map(
        "name" -> dataPropName,
        "description" -> dataPropDescription
      )
    )
  )

  private val dataPropBool = DataModelProperty("boolean", true)
  private val dataPropFloat = DataModelProperty("float64", true)

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

  private def insertDataModels() = {
    val outputCreates =
      blueFieldClient.dataModels
        .createItems(Items[DataModel](Seq(dataModel1, dataModel2)))
        .unsafeRunSync()
        .toList
    outputCreates.size should be >= 2

    retryWithExpectedResult[scala.collection.Seq[DataModel]](
      blueFieldClient.dataModels.list().unsafeRunSync(),
      dm => dm.contains(dataModel1) && dm.contains(dataModel2) shouldBe true
    )
    outputCreates
  }

  private def deleteDataModels() = {
    blueFieldClient.dataModels
      .deleteItems(Seq(dataModel1.externalId, dataModel2.externalId))
      .unsafeRunSync()

    retryWithExpectedResult[scala.collection.Seq[DataModel]](
      blueFieldClient.dataModels.list().unsafeRunSync(),
      dm => dm.contains(dataModel1) && dm.contains(dataModel2) shouldBe false
    )
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

  "Get data models definitions by ids" should "work for multiple externalIds" in initAndCleanUpData {
    _ =>
      val outputGetByIds =
        blueFieldClient.dataModels
          .retrieveByExternalIds(
            Seq(dataModel1.externalId, dataModel2.externalId),
            false,
            false
          )
          .unsafeRunSync()
          .toList
      outputGetByIds.size shouldBe 2
    // VH TODO Fix this when the api is updated
    /*val expectedDataModelsOutputWithProps = Seq(dataModel1, dataModel2).map { dm =>
        dm.copy(properties =
          dm.properties.map(x => x ++ Map("externalId" -> DataModelProperty("text", false)))
        )
      }
      outputGetByIds.toSet shouldBe expectedDataModelsOutputWithProps.toSet*/
  }

  // VH TODO Fix this when the api is updated
  ignore should "work with include inherited properties" in initAndCleanUpData { _ =>
    val expectedDataModel1 =
      dataModel1.copy(properties =
        dataModel1.properties.map(x => x ++ Map("externalId" -> DataModelProperty("text", false)))
      )

    val expectedDataModel2 =
      dataModel2.copy(properties =
        dataModel2.properties.map(x =>
          x ++ Map("externalId" -> DataModelProperty("text", false)) ++ dataModel1.properties
            .getOrElse(Map())
        )
      )

    val outputGetByIds =
      blueFieldClient.dataModels
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
      blueFieldClient.dataModels
        .retrieveByExternalIds(
          Seq("toto"),
          ignoreUnknownIds = true
        )
        .unsafeRunSync()
        .toList
    outputIgnoreUnknownIds.isEmpty shouldBe true

    an[SdkException] should be thrownBy {
      blueFieldClient.dataModels
        .retrieveByExternalIds(
          Seq("toto"),
          ignoreUnknownIds = false
        )
        .unsafeRunSync()
    }
  }
}
