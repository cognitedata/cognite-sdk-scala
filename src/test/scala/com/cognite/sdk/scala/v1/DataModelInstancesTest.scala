// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1

import com.cognite.sdk.scala.common.{CdpApiException, Items, RetryWhile}
import io.circe.Json
import org.scalatest.BeforeAndAfterAll

import java.util.UUID

//import cats.Id
//import sttp.client3.testing.SttpBackendStub
//import sttp.model.{Header, MediaType, Method, StatusCode}

import scala.collection.immutable.Seq

@SuppressWarnings(
  Array(
    "org.wartremover.warts.PublicInference",
    "org.wartremover.warts.NonUnitStatements"
  )
)
class DataModelInstancesTest
    extends CommonDataModelTestHelper
    with RetryWhile
    with BeforeAndAfterAll {
  val uuid = UUID.randomUUID.toString
  val dataPropString = DataModelProperty("text", Some(true))
  val dataPropBool = DataModelProperty("boolean", Some(true))
  val dataPropFloat =
    DataModelProperty("float64", Some(true)) // VH TODO change this to false to test

  val dataModel = DataModel(
    s"Equipment-${uuid.substring(0, 8)}",
    Some(
      Map(
        "prop_string" -> dataPropString,
        "prop_bool" -> dataPropBool,
        "prop_float" -> dataPropFloat
      )
    )
  )

  val dataModelInstanceToCreate1 =
    DataModelInstance(
      dataModel.externalId,
      Some(
        Map(
          "externalId" -> Json.fromString("equipment_43"),
          "prop_string" -> Json.fromString("EQ0001"),
          "prop_float" -> Json.fromDoubleOrNull(0)
        )
      )
    )

  val dataModelInstanceToCreate2 =
    DataModelInstance(
      dataModel.externalId,
      Some(
        Map(
          "externalId" -> Json.fromString("equipment_44"),
          "prop_string" -> Json.fromString("EQ0002"),
          "prop_bool" -> Json.fromBoolean(true),
          "prop_float" -> Json.fromDoubleOrNull(1.64)
        )
      )
    )

  val dataModelInstanceToCreate3 =
    DataModelInstance(
      dataModel.externalId,
      Some(
        Map(
          "externalId" -> Json.fromString("equipment_45"),
          "prop_string" -> Json.fromString("EQ0011"),
          "prop_bool" -> Json.fromBoolean(false),
          "prop_float" -> Json.fromDoubleOrNull(3.5)
        )
      )
    )

  val toCreates =
    Seq(dataModelInstanceToCreate1, dataModelInstanceToCreate2, dataModelInstanceToCreate3)

  override def beforeAll(): Unit = {
    val dataModels =
      blueFieldClient.dataModels
        .createItems(Items[DataModel](Seq(dataModel)))
        .unsafeRunSync()
        .toList
    dataModels.contains(dataModel) shouldBe true
    ()
  }

  override def afterAll(): Unit = {
    blueFieldClient.dataModels.deleteItems(Seq(dataModel.externalId)).unsafeRunSync()
    val dataModels = blueFieldClient.dataModels.list().unsafeRunSync().toList
    dataModels.contains(dataModel) shouldBe false
    ()
  }

  "Insert data model instances" should "work with multiple input" in {
    val dataModelInstances = blueFieldClient.dataModelInstances
      .createItems(
        Items[DataModelInstance](
          toCreates
        )
      )
      .unsafeRunSync()
      .toList

    dataModelInstances.size shouldBe 3
    dataModelInstances.map(_.properties).toSet shouldBe toCreates.map(_.properties).toSet

    // VH TODO remove this once 500 hitting rate is stable
    /*val expectedBody = StringBody(
      s"""{"items":[{"modelExternalId":"${dataModelInstanceToCreate.externalId}",
      "properties":{"prop_string":{"type":"text","nullable":true},
      "description":{"type":"text","nullable":true}}}]}""".stripMargin,
      "utf-8",
      MediaType.ApplicationJson
    )

    val expectedResponse = Seq(dataModelInstanceToCreate)
    val responseForDataModelCreated = SttpBackendStub.synchronous
      .whenRequestMatches { r =>
        r.method === Method.POST && r.uri.path.endsWith(
          List("instances", "ingest")
        )
      }
      .thenRespond(
        Response(
          expectedResponse,
          StatusCode.Ok,
          "OK",
          Seq(Header("content-type", "application/json; charset=utf-8"))
        )
      )

    val client = new GenericClient[Id](
      applicationName = "CogniteScalaSDK-OAuth-Test",
      projectName = "session-testing",
      auth = BearerTokenAuth("bearer Token"),
      cdfVersion = Some("alpha")
    )(implicitly, responseForDataModelCreated)

    val resCreate = client.dataModelInstances.createItems(
      Items[DataModelInstance](Seq(dataModelInstanceToCreate))
    )
    resCreate shouldBe expectedResponse*/
  }

  it should "fail if input data type is not correct" in {
    val invalidInput = DataModelInstance(
      dataModel.externalId,
      Some(
        Map(
          "externalId" -> Json.fromString("equipment_47"),
          "prop_float" -> Json.fromString("abc")
        )
      )
    )
    val exception = the[CdpApiException] thrownBy blueFieldClient.dataModelInstances
      .createItems(
        Items[DataModelInstance](
          Seq(invalidInput)
        )
      )
      .unsafeRunSync()

    // TODO change this when ingest api return better error detail
    exception.message.contains("Internal server error. Please report this error to") shouldBe true

  }

  private def insertDMIBeforeQuery() = {
    val dataModelInstances = blueFieldClient.dataModelInstances
      .createItems(
        Items[DataModelInstance](
          toCreates
        )
      )
      .unsafeRunSync()
      .toList
    dataModelInstances.size shouldBe 3
    dataModelInstances
  }

  private def deleteDMIAfterQuery() = {
    val toDeletes =
      toCreates.flatMap(_.properties).flatMap(_.get("externalId").map(_.asString.getOrElse("")))
    blueFieldClient.dataModelInstances
      .deleteByExternalIds(toDeletes)
      .unsafeRunSync()

    // make sure that data is deleted
    val inputNoFilterQuery = DataModelInstanceQuery(dataModel.externalId)
    val outputNoFilter = blueFieldClient.dataModelInstances
      .query(inputNoFilterQuery)
      .unsafeRunSync()
      .items
      .toList
    outputNoFilter.isEmpty shouldBe true
  }

  private def initAndCleanUpDataForQuery(testCode: Seq[DataModelInstance] => Any): Unit =
    try {
      val dataModelInstances: Seq[DataModelInstance] = insertDMIBeforeQuery()
      val _ = testCode(dataModelInstances)
    } catch {
      case t: Throwable => throw t
    } finally {
      deleteDMIAfterQuery()
      ()
    }

  "Query data model instances" should "work with empty filter" in initAndCleanUpDataForQuery { _ =>
    val inputNoFilterQuery = DataModelInstanceQuery(dataModel.externalId)
    val outputNoFilter = blueFieldClient.dataModelInstances
      .query(inputNoFilterQuery)
      .unsafeRunSync()
    outputNoFilter.items.toList.size shouldBe 3
  }

  it should "work with AND filter" in initAndCleanUpDataForQuery { _ =>
    val inputQueryAnd = DataModelInstanceQuery(
      dataModel.externalId,
      Some(
        DMIAndFilter(
          Seq(
            DMIEqualsFilter(Seq(dataModel.externalId, "prop_string"), Json.fromString("EQ0002")),
            DMIEqualsFilter(Seq(dataModel.externalId, "prop_bool"), Json.fromBoolean(true)),
            DMIEqualsFilter(Seq(dataModel.externalId, "prop_float"), Json.fromFloatOrNull(1.64f))
          )
        )
      )
    )
    val outputQueryAnd = blueFieldClient.dataModelInstances
      .query(inputQueryAnd)
      .unsafeRunSync()
      .items
      .toList

    outputQueryAnd.size shouldBe 1
    outputQueryAnd.map(_.properties).toSet shouldBe Set(dataModelInstanceToCreate2.properties)

    val inputQueryAnd2 = DataModelInstanceQuery(
      dataModel.externalId,
      Some(
        DMIAndFilter(
          Seq(
            DMIEqualsFilter(Seq(dataModel.externalId, "prop_string"), Json.fromString("EQ0001")),
            DMIEqualsFilter(Seq(dataModel.externalId, "prop_bool"), Json.fromBoolean(true))
          )
        )
      )
    )
    val outputQueryAndEmpty = blueFieldClient.dataModelInstances
      .query(inputQueryAnd2)
      .unsafeRunSync()
      .items
      .toList

    outputQueryAndEmpty.isEmpty shouldBe true
  }

  it should "work with OR filter" in initAndCleanUpDataForQuery { _ =>
    val inputQueryOr = DataModelInstanceQuery(
      dataModel.externalId,
      Some(
        DMIOrFilter(
          Seq(
            DMIEqualsFilter(Seq(dataModel.externalId, "prop_string"), Json.fromString("EQ0011")),
            DMIEqualsFilter(Seq(dataModel.externalId, "prop_bool"), Json.fromBoolean(true))
          )
        )
      )
    )
    val outputQueryOr = blueFieldClient.dataModelInstances
      .query(inputQueryOr)
      .unsafeRunSync()
      .items
      .toList

    outputQueryOr.size shouldBe 2
    outputQueryOr.map(_.properties).toSet shouldBe Set(
      dataModelInstanceToCreate2.properties,
      dataModelInstanceToCreate3.properties
    )
  }

  it should "work with NOT filter" in initAndCleanUpDataForQuery { _ =>
    val inputQueryNot = DataModelInstanceQuery(
      dataModel.externalId,
      Some(
        DMINotFilter(
          DMIInFilter(
            Seq(dataModel.externalId, "prop_string"),
            Seq(Json.fromString("EQ0002"), Json.fromString("EQ0011"))
          )
        )
      )
    )
    val outputQueryNot = blueFieldClient.dataModelInstances
      .query(inputQueryNot)
      .unsafeRunSync()
      .items
      .toList

    outputQueryNot.size shouldBe 1
    outputQueryNot.map(_.properties).toSet shouldBe Set(dataModelInstanceToCreate1.properties)
  }

  it should "work with PREFIX filter" in initAndCleanUpDataForQuery { _ =>
    val inputQueryPrefix = DataModelInstanceQuery(
      dataModel.externalId,
      Some(
        DMIPrefixFilter(Seq(dataModel.externalId, "prop_string"), Json.fromString("EQ000"))
      )
    )
    val outputQueryPrefix = blueFieldClient.dataModelInstances
      .query(inputQueryPrefix)
      .unsafeRunSync()
      .items
      .toList

    outputQueryPrefix.size shouldBe 2
    outputQueryPrefix.map(_.properties).toSet shouldBe Set(
      dataModelInstanceToCreate1.properties,
      dataModelInstanceToCreate2.properties
    )
  }

  it should "work with RANGE filter" in initAndCleanUpDataForQuery { _ =>
    val inputQueryRange = DataModelInstanceQuery(
      dataModel.externalId,
      Some(
        DMIRangeFilter(
          Seq(dataModel.externalId, "prop_float"),
          gte = Some(Json.fromFloatOrNull(1.64f))
        )
      )
    )
    val outputQueryRange = blueFieldClient.dataModelInstances
      .query(inputQueryRange)
      .unsafeRunSync()
      .items
      .toList

    outputQueryRange.map(_.properties).toSet shouldBe Set(
      dataModelInstanceToCreate2.properties,
      dataModelInstanceToCreate3.properties
    )
  }

  it should "work with EXISTS filter" in initAndCleanUpDataForQuery { _ =>
    val inputQueryExists = DataModelInstanceQuery(
      dataModel.externalId,
      Some(
        DMIExistsFilter(Seq(dataModel.externalId, "prop_bool"))
      )
    )
    val outputQueryExists = blueFieldClient.dataModelInstances
      .query(inputQueryExists)
      .unsafeRunSync()
      .items
      .toList

    outputQueryExists.map(_.properties).toSet shouldBe Set(
      dataModelInstanceToCreate2,
      dataModelInstanceToCreate3
    ).map(_.properties)
  }

  // Not yet supported
  ignore should "work with CONTAINS ANY filter" in initAndCleanUpDataForQuery { _ =>
    val inputQueryContainsAny = DataModelInstanceQuery(
      dataModel.externalId,
      Some(
        DMIContainsAnyFilter(
          Seq(dataModel.externalId, "prop_float"),
          Seq(Json.fromDoubleOrNull(0), Json.fromFloatOrNull(3.5f))
        )
      )
    )
    val outputQueryContainsAny = blueFieldClient.dataModelInstances
      .query(inputQueryContainsAny)
      .unsafeRunSync()
      .items
      .toList

    outputQueryContainsAny.map(_.properties).toSet shouldBe Set(
      dataModelInstanceToCreate1,
      dataModelInstanceToCreate3
    ).map(_.properties)
  }

  // Not yet supported
  ignore should "work with CONTAINS ALL filter" in initAndCleanUpDataForQuery { _ =>
    val inputQueryContainsAll = DataModelInstanceQuery(
      dataModel.externalId,
      Some(
        DMIContainsAllFilter(
          Seq(dataModel.externalId, "prop_float", "prop_string"),
          Seq(Json.fromDoubleOrNull(0), Json.fromString("EQ0001"))
        )
      )
    )
    val outputQueryContainsAll = blueFieldClient.dataModelInstances
      .query(inputQueryContainsAll)
      .unsafeRunSync()
      .items
      .toList

    outputQueryContainsAll.map(_.properties).toSet shouldBe Set(
      dataModelInstanceToCreate1
    ).map(_.properties)
  }

  // Not yet supported
  ignore should "work with sort" in initAndCleanUpDataForQuery { _ =>
    val inputQueryExists = DataModelInstanceQuery(
      dataModel.externalId,
      Some(
        DMIExistsFilter(Seq(dataModel.externalId, "prop_float"))
      ),
      Some(Seq(dataModel.externalId, "col_float:desc"))
    )
    val outputQueryExists = blueFieldClient.dataModelInstances
      .query(inputQueryExists)
      .unsafeRunSync()
      .items
      .toList

    outputQueryExists.map(_.properties) shouldBe Seq(
      dataModelInstanceToCreate3,
      dataModelInstanceToCreate2,
      dataModelInstanceToCreate1
    ).map(_.properties)
  }

  it should "work with limit" in initAndCleanUpDataForQuery { _ =>
    val inputQueryOr = DataModelInstanceQuery(
      dataModel.externalId,
      Some(
        DMIOrFilter(
          Seq(
            DMIEqualsFilter(Seq(dataModel.externalId, "prop_string"), Json.fromString("EQ0011")),
            DMIEqualsFilter(Seq(dataModel.externalId, "prop_bool"), Json.fromBoolean(true))
          )
        )
      ),
      None,
      Some(1)
    )
    val outputQueryOr = blueFieldClient.dataModelInstances
      .query(inputQueryOr)
      .unsafeRunSync()
      .items
      .toList

    outputQueryOr.size shouldBe 1
    outputQueryOr
      .map(_.properties)
      .toSet
      .subsetOf(
        Set(
          dataModelInstanceToCreate2,
          dataModelInstanceToCreate3
        ).map(_.properties)
      ) shouldBe true
  }

  // Not yet supported
  "List data model instances" should "work with multiple externalIds" ignore {
    val toGets = toCreates.map { d =>
      DataModelInstanceByExternalId(
        d.properties.flatMap(_.get("externalId")).flatMap(_.asString).getOrElse(""),
        d.modelExternalId
      )
    }
    val outputList = blueFieldClient.dataModelInstances
      .retrieveByExternalIds(toGets, false)
      .unsafeRunSync()
      .toList
    outputList.size shouldBe 3
    outputList.map(_.properties).toSet shouldBe toCreates.map(_.properties).toSet
  }

  // Not yet supported
  ignore should "raise an exception if input has invalid externalId and ignoreUnknownIds is false" in {
    the[CdpApiException] thrownBy blueFieldClient.dataModelInstances
      .retrieveByExternalIds(
        Seq(DataModelInstanceByExternalId(dataModel.externalId, "toto")),
        false
      )
      .unsafeRunSync()
      .toList
  }

  // Not yet supported
  ignore should "ignore if input has invalid externalId and ignoreUnknownIds is true" in {
    val res = blueFieldClient.dataModelInstances
      .retrieveByExternalIds(
        Seq(DataModelInstanceByExternalId(dataModel.externalId, "toto")),
        true
      )
      .unsafeRunSync()
      .toList

    res.isEmpty shouldBe true
  }

  "Delete data model instances" should "work with multiple externalIds" in {
    val toDeletes =
      toCreates.flatMap(_.properties).flatMap(_.get("externalId").map(_.asString.getOrElse("")))

    blueFieldClient.dataModelInstances
      .deleteByExternalIds(toDeletes)
      .unsafeRunSync()

    // make sure that data is deleted
    val inputNoFilterQuery = DataModelInstanceQuery(dataModel.externalId)
    val outputNoFilter = blueFieldClient.dataModelInstances
      .query(inputNoFilterQuery)
      .unsafeRunSync()
      .items
      .toList
    outputNoFilter.isEmpty shouldBe true
  }

  it should "ignore unknown externalId" in {
    noException should be thrownBy blueFieldClient.dataModelInstances
      .deleteByExternalIds(Seq("toto"))
      .unsafeRunSync()
  }
}
