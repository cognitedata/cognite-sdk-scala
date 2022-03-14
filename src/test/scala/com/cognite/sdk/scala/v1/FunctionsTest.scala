// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1

import io.circe.Json
import org.scalatest.Inspectors._
import io.circe.JsonObject
import com.cognite.sdk.scala.common._

@SuppressWarnings(Array("org.wartremover.warts.TraversableOps", "org.wartremover.warts.NonUnitStatements"))
class FunctionsTest extends SdkTestSpec with ReadBehaviours {
  override lazy val client: GenericClient[cats.Id] = GenericClient.forAuth[cats.Id](
    "playground", auth, apiVersion = Some("playground"))(implicitly, sttpBackend)

  ignore should "read function items" in {
    client.functions.read().items should not be empty
  }

  ignore should "retrieve function by id" in {
    val func = client.functions.retrieveById(13109660923970L)
    func.id should equal(Some(13109660923970L))
    func.name should equal("Scala SDK test function")
    func.status should equal(Some("Ready"))
  }

  ignore should "create and delete functions" in {
    val createRes = client.functions.create(Seq(
      FunctionCreate(
        name = "scala-sdk-write-example-1",
        fileId = 523869369979314L
      )
    ))
    createRes.length should equal(1)
    client.functions.deleteByIds(createRes.map(_.id.getOrElse(0L)))
  }

  ignore should "read function call items" in {
    client.functionCalls(13109660923970L).read().items should not be empty
  }

  ignore should "retrieve function call by id" in {
    val call = client.functionCalls(13109660923970L).retrieveById(4177407523317994L)
    call.id should equal(Some(4177407523317994L))
    call.status should equal(Some("Completed"))
  }

  ignore should "read function call logs items" in {
    val res = client.functionCalls(13109660923970L).retrieveLogs(4177407523317994L)
    res.items should not be empty
    val log = res.items.head
    log.message should equal(Some("{}"))
  }

  ignore should "retrieve function call response" in {
    val response = client.functionCalls(13109660923970L).retrieveResponse(4177407523317994L)
    response.response should equal(Some(Json.fromInt(42)))
  }

  ignore should "filter function calls" in {
    val filter = FunctionCallFilter(status = Some("Completed"))
    val res = client.functionCalls(13109660923970L).filter(filter).items
    forAll (res) { call => call.status should equal(Some("Completed")) }
  }

  ignore should "read function schedule items" in {
    val res = client.functionSchedules.read()
    res.items should not be empty
    val schedule = res.items.head
    schedule.name should equal("Scala SDK test schedule")
    schedule.cronExpression should equal(Some("0 0 1 * *"))
  }

  ignore should "call function" in {
    val res = client.functionCalls(13109660923970L).callFunction(Json.fromJsonObject(JsonObject.empty))
    res.status should equal(Some("Running"))
  }

  ignore should "create and delete function schedules" in {
    val createRes = client.functionSchedules.create(Seq(
      FunctionScheduleCreate(
        name = "scala-sdk-write-example-1",
        cronExpression = "0 0 1 * *"
      )
    ))
    createRes.length should equal(1)
    client.functionSchedules.deleteByIds(createRes.map(_.id.getOrElse(0L)))
  }
}
