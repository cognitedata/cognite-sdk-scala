// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1

import cats.effect.unsafe.implicits.global
import com.cognite.sdk.scala.common.RetryWhile

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
  val requiredTextProperty = DataModelPropertyDeffinition(PropertyType.Text, false)
  val dataPropDescription = DataModelPropertyDeffinition(PropertyType.Text)
  val dataPropDirectRelation = DataModelPropertyDeffinition(PropertyType.DirectRelation)
  // val dataPropIndex = DataModelPropertyIndex(Some("name_descr"), Some(Seq("name", "description")))

  val dataModel = DataModel(
    s"Equipment-${uuid.substring(0, 8)}",
    Some(
      Map(
        "name" -> requiredTextProperty,
        "description" -> dataPropDescription,
        "parentExternalId" -> dataPropDirectRelation
      )
    )
  )

  val expectedDataModelOutput = dataModel.copy(properties =
    dataModel.properties.map(x =>
      x ++ Map("externalId" -> DataModelPropertyDeffinition(PropertyType.Text, false))
    )
  )

  // TODO: enable model creation test when fdm team enables delete
  "DataModels" should "create data models definitions" ignore {
    val dataModels =
      blueFieldClient.dataModels
        .createItems(Seq(dataModel), space)
        .unsafeRunSync()
        .toList
    dataModels.contains(expectedDataModelOutput) shouldBe true
  }

  it should "list all data models definitions" in initAndCleanUpData {
    _ => {
      val dataModels = blueFieldClient.dataModels.list(space).unsafeRunSync().toList
      dataModels.nonEmpty shouldBe true
      dataModels.contains(dataModel1) shouldBe true
      dataModels.contains(expectedDataModel2Output) shouldBe true
    }
  }

  // TODO: cleanup and re-enable delete test when fdm team enables delete
  ignore should "delete data models definitions" in initAndCleanUpData { _ =>
    blueFieldClient.dataModels.deleteItems(Seq(dataModel.externalId), space).unsafeRunSync()

    val dataModels = blueFieldClient.dataModels.list(space).unsafeRunSync().toList
    dataModels.contains(expectedDataModelOutput) shouldBe false
  }

  private def variousDatamodels(testCode: DataModel => Any): Unit =
    for (dataModel <- Seq(
      DataModel("testNode", dataModelType = DataModelType.Node),
      DataModel("testEdge", dataModelType = DataModelType.Edge)
    )){
      testCode(dataModel)
    }

  "dataModelEncoder" should "add allowNode and allowEdge and remove dataModelType" in variousDatamodels{ dataModel =>
    import resources.DataModels._
    val json = dataModelEncoder(dataModel)

    val (allowNode, allowEdge) = dataModel.dataModelType match {
      case DataModelType.Edge => (false, true)
      case DataModelType.Node => (true, false)
    }

    json.asObject.flatMap(_("allowNode")).flatMap(_.asBoolean) shouldBe Some(allowNode)
    json.asObject.flatMap(_("allowEdge")).flatMap(_.asBoolean) shouldBe Some(allowEdge)
    json.asObject.flatMap(_("dataModelType")) shouldBe None
  }

  "dataModelDecoder" should "add back dataModelType" in variousDatamodels{ dataModel =>
    import resources.DataModels._

    dataModelDecoder(dataModelEncoder(dataModel).hcursor).toOption shouldBe Some(dataModel)
  }

  private val space = "test-space"

  private val dataModel1 = DataModel(
    // TODO: enable transient datamodel tests when fdm team enables delete
    // s"Equipment-${UUID.randomUUID.toString.substring(0, 8)}",
    s"Equipment",
    Some(
      Map(
        "name" -> requiredTextProperty,
        "externalId" -> requiredTextProperty
      )
    )
  )

  private val dataPropBool = DataModelPropertyDeffinition(PropertyType.Boolean, true)
  private val dataPropFloat = DataModelPropertyDeffinition(PropertyType.Float64, true)

  private val dataModel2 = DataModel(
    // TODO: enable transient datamodel tests when fdm team enables delete
    // s"Equipment-${UUID.randomUUID.toString.substring(0, 8)}",
    s"SpecialEquipment",
    Some(
      Map(
        "prop_bool" -> dataPropBool,
        "prop_float" -> dataPropFloat
      )
    ),
    Some(Seq(DataModelIdentifier(Some(space), dataModel1.externalId)))
  )

  val expectedDataModel2Output = dataModel2.copy(properties =
    dataModel2.properties.map(x =>
      x ++ dataModel1.properties.getOrElse(Map())
    )
  )

  private def insertDataModels() = {

    val outputCreates =
    // TODO: enable transient datamodel tests when fdm team enables delete
    /*
      blueFieldClient.dataModels
        .createItems(Seq(dataModel1, dataModel2), space)
        .unsafeRunSync()
        .toList
    */
      Seq(dataModel1, expectedDataModel2Output)

    outputCreates.size should be >= 2

    retryWithExpectedResult[scala.collection.Seq[DataModel]](
      blueFieldClient.dataModels.list(space).unsafeRunSync(),
      dm => dm.contains(dataModel1) && dm.contains(expectedDataModel2Output) shouldBe true
    )
    outputCreates
  }

  private def deleteDataModels(): Unit = {
    // TODO: enable transient datamodel tests when fdm team enables delete
    /*
    blueFieldClient.dataModels
      .deleteItems(Seq(dataModel1.externalId, dataModel2.externalId), space)
      .unsafeRunSync()

    retryWithExpectedResult[scala.collection.Seq[DataModel]](
      blueFieldClient.dataModels.list(space).unsafeRunSync(),
      dm => dm.contains(dataModel1) && dm.contains(dataModel2) shouldBe false
    )
    */
    ()
  }

  private def initAndCleanUpData(testCode: Seq[DataModel] => Any): Unit =
    try {
      val dataModelInstances: Seq[DataModel] = insertDataModels()
      val _ = testCode(dataModelInstances)
    } catch {
      case t: Throwable => throw t
    } finally {
      deleteDataModels()
    }

  "Get data models definitions by ids" should "work for multiple externalIds" in initAndCleanUpData {
    _ =>
      val outputGetByIds =
        blueFieldClient.dataModels
          .retrieveByExternalIds(
            Seq(dataModel1.externalId, dataModel2.externalId),
            space
          )
          .unsafeRunSync()
          .toList
      outputGetByIds.size shouldBe 2
      val expectedDataModelsOutputWithProps = Seq(dataModel1, expectedDataModel2Output)
      outputGetByIds.toSet shouldBe expectedDataModelsOutputWithProps.toSet
  }

}
