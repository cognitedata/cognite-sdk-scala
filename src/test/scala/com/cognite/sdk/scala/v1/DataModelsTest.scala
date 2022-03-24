// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1

import cats.effect.unsafe.implicits.global
import com.cognite.sdk.scala.common.CdpApiException
import com.cognite.sdk.scala.common.{Items, RetryWhile}

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
  val dataPropName = DataModelProperty(PropertyName.Text, false)
  val dataPropDescription = DataModelProperty(PropertyName.Text)
  val dataPropDirectRelation = DataModelProperty(PropertyName.DirectRelation)
  // val dataPropIndex = DataModelPropertyIndex(Some("name_descr"), Some(Seq("name", "description")))

  val dataModel = DataModel(
    s"Equipment-${uuid.substring(0, 8)}",
    Some(
      Map(
        "name" -> dataPropName,
        "description" -> dataPropDescription,
        "parentExternalId" -> dataPropDirectRelation
      )
    ),
    None, // Some(Seq("Asset", "Pump")),
    None // Some(Seq(dataPropIndex))
  )

  val expectedDataModelOutput = dataModel.copy(properties =
    dataModel.properties.map(x =>
      x ++ Map("externalId" -> DataModelProperty(PropertyName.Text, false))
    )
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

  private val dataPropBool = DataModelProperty(PropertyName.Boolean, true)
  private val dataPropFloat = DataModelProperty(PropertyName.Float64, true)

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
      val expectedDataModelsOutputWithProps = Seq(dataModel1, dataModel2)
      outputGetByIds.toSet shouldBe expectedDataModelsOutputWithProps.toSet
  }

  // VH TODO Fix this when the api is updated as it does not return "extends" field yet
  ignore should "work with include inherited properties" in initAndCleanUpData { _ =>
    val expectedDataModel1 =
      dataModel1.copy(properties =
        dataModel1.properties.map(x =>
          x ++ Map("externalId" -> DataModelProperty(PropertyName.Text, false))
        )
      )

    val expectedDataModel2 =
      dataModel2.copy(properties =
        dataModel2.properties.map(x =>
          x ++ Map(
            "externalId" -> DataModelProperty(PropertyName.Text, false)
          ) ++ dataModel1.properties
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
    val unknownId = "toto"
    val outputIgnoreUnknownIds =
      blueFieldClient.dataModels
        .retrieveByExternalIds(
          Seq(unknownId),
          ignoreUnknownIds = true
        )
        .unsafeRunSync()
        .toList
    outputIgnoreUnknownIds.isEmpty shouldBe true

    val error = the[CdpApiException] thrownBy {
      blueFieldClient.dataModels
        .retrieveByExternalIds(
          Seq(unknownId),
          ignoreUnknownIds = false
        )
        .unsafeRunSync()
    }
    error.message shouldBe s"ids not found: $unknownId"
  }
}
