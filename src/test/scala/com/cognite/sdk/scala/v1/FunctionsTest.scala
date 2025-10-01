// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1

import cats.effect.unsafe.implicits.global
import com.cognite.sdk.scala.common._
import io.circe.{Json, JsonObject}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.Inspectors._
import org.scalatest.matchers.should.Matchers


@SuppressWarnings(
  Array(
    "org.wartremover.warts.TraversableOps",
    "org.wartremover.warts.NonUnitStatements",
    "org.wartremover.warts.IterableOps"
  )
)
class FunctionsTest extends CommonDataModelTestHelper with Matchers with ReadBehaviours with BeforeAndAfterAll {
  private val client = testClient

  private def getNonce: String = client.sessions
    .createWithClientCredentialFlow(Items(Seq(SessionCreateWithCredential(credentials.clientId, credentials.clientSecret)))).unsafeRunSync()
    .map(_.nonce).head

  //Further test assume this function already exists
  private val preExistingFunctionId: Long = 8590831424885479L

  //Create and completes a call
  //Due to 90 days retention policy, this needs to be dynamic
  private lazy val callId: Long = {
    val call = client.functionCalls(preExistingFunctionId).callFunction(Json.fromJsonObject(JsonObject.empty), Some(getNonce)).unsafeRunSync()
    retryWithExpectedResult[FunctionCall](
      client.functionCalls(preExistingFunctionId).retrieveById(call.id).unsafeRunSync(),
      _.status should equal("Completed"),
      retriesRemaining = 5
    )
    call.id
  }

  it should "read function items" in {
    client.functions.read().unsafeRunSync().items should not be empty
  }

  it should "retrieve function by id" in {
    val func = client.functions.retrieveById(preExistingFunctionId).unsafeRunSync()
    func.id should equal(Some(preExistingFunctionId))
    func.name should equal("scala-sdk-test-function")
    func.status should equal(Some("Ready"))
  }

  ignore should "create and delete functions" in {
    val createRes = client.functions.create(Seq(
      FunctionCreate(
        name = "scala-sdk-write-example-1",
        fileId = 523869369979314L
      )
    )).unsafeRunSync()
    createRes.length should equal(1)
    client.functions.deleteByIds(createRes.map(_.id.getOrElse(0L)))
  }

  it should "read function call items" in {
    client.functionCalls(preExistingFunctionId).read().unsafeRunSync().items should not be empty
  }

  it should "retrieve function call by id" in {
    val call = client.functionCalls(preExistingFunctionId).retrieveById(callId).unsafeRunSync()
    call.id should equal(callId)
  }

  it should "read function call logs items" in {
    val res = client.functionCalls(preExistingFunctionId).retrieveLogs(callId).unsafeRunSync()
    res.items.isEmpty shouldBe true
  }

  it should "retrieve function call response" in {
    val response = client.functionCalls(preExistingFunctionId).retrieveResponse(callId).unsafeRunSync()
    response.response shouldBe Some(Json.fromFields(Seq(("res_int", Json.fromInt(1)),("res_string", Json.fromString("string response")))))
  }

  it should "filter function calls" in {
    val filter = FunctionCallFilter(status = Some("Completed"))
    val res = client.functionCalls(preExistingFunctionId).filter(filter).unsafeRunSync().items
    forAll (res) { call => call.status should equal("Completed") }
  }

  it should "read function schedule items" in {
    val res = client.functionSchedules.read().unsafeRunSync()
    res.items should not be empty
    val schedule = res.items.head
    schedule.name should equal("test-schedule-function")
    schedule.cronExpression should equal(Some("0 0 1 * *"))
  }

  it should "call function" in {
    val res = client.functionCalls(preExistingFunctionId).callFunction(Json.fromJsonObject(JsonObject.empty), Some(getNonce)).unsafeRunSync()
    res.status should equal("Running")
  }

  it should "create and delete function schedules" in {
    val createRes = client.functionSchedules.create(Seq(
      FunctionScheduleCreate(
        name = "scala-sdk-write-example-1",
        functionId = Some(preExistingFunctionId),
        cronExpression = "0 0 1 * *",
        nonce = Some(getNonce)
      )
    )).unsafeRunSync()
    createRes.length should equal(1)
    client.functionSchedules.deleteByIds(createRes.map(_.id.getOrElse(0L))).unsafeRunSync()
    client.functionSchedules.read().unsafeRunSync().items.find(_.name === "scala-sdk-write-example-1") shouldBe None
  }
}
