// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1

//import cats.Id
import com.cognite.sdk.scala.common.{Items, RetryWhile}
//import sttp.client3.testing.SttpBackendStub
//import sttp.model.{Header, MediaType, Method, StatusCode}

import java.util.UUID
import scala.collection.immutable.Seq

@SuppressWarnings(
  Array(
    "org.wartremover.warts.NonUnitStatements"
  )
)
class DataModelsTest extends CommonDataModelTestHelper with RetryWhile {

  "DataModels" should "create data models definitions" in {
    val uuid = UUID.randomUUID.toString
    val dataPropName = DataModelProperty("text", Some(true))
    val dataPropDescription = DataModelProperty("text", Some(true))
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

    val dataModels =
      blueFieldClient.dataModels
        .createItems(Items[DataModel](Seq(dataModel)))
        .unsafeRunSync()
        .toList
    dataModels.contains(dataModel) shouldBe true

    // VH TODO remove the code below and use real test above when create endpoints doesn't return 500 anymore
    /*val expectedBody = StringBody(
      s"""{"items":[{"externalId":"${dataModel.externalId}",
      "properties":{"name":{"type":"text","nullable":true},
      "description":{"type":"text","nullable":true}}}]}""".stripMargin,
      "utf-8",
      MediaType.ApplicationJson
    )
    val expectedResponse = Seq(dataModel)
    val responseForDataModelCreated = SttpBackendStub.synchronous
      .whenRequestMatches { r =>
        r.method === Method.POST && r.uri.path.endsWith(
          List("definitions", "apply")
        ) && (r.body.equals(expectedBody))
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

    val resCreate = client.dataModels.createItems(
      Items[DataModel](Seq(dataModel))
    )
    resCreate shouldBe expectedResponse*/
  }

  it should "list all data models definitions" in {
    val dataModels = blueFieldClient.dataModels.list().unsafeRunSync().toList
    dataModels.nonEmpty shouldBe true
  }

  ignore should "delete data models definitions" in {
    val uuid = UUID.randomUUID.toString
    val dataPropName = DataModelProperty("text", Some(true))
    val dataPropDescription = DataModelProperty("text", Some(true))

    val newDataModel = DataModel(
      s"Equipment-${uuid.substring(0, 8)}",
      Some(
        Map(
          "name" -> dataPropName,
          "description" -> dataPropDescription
        )
      )
    )

    val outputCrates =
      blueFieldClient.dataModels
        .createItems(Items[DataModel](Seq(newDataModel)))
        .unsafeRunSync()
        .toList
    outputCrates.contains(newDataModel) shouldBe true

    blueFieldClient.dataModels.deleteItems(Seq(newDataModel.externalId), true).unsafeRunSync()

    val dataModels = blueFieldClient.dataModels.list().unsafeRunSync().toList
    dataModels.contains(newDataModel) shouldBe false
  }

}
