// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import io.circe.Json
import org.scalatest.Inspectors._
import io.circe.JsonObject
import com.cognite.sdk.scala.common._
import org.scalatest.matchers.should.Matchers


@SuppressWarnings(
  Array(
    "org.wartremover.warts.TraversableOps",
    "org.wartremover.warts.NonUnitStatements",
    "org.wartremover.warts.IterableOps"
  )
)
class FunctionsTest extends CommonDataModelTestHelper with Matchers with ReadBehaviours {

  private val client = new GenericClient[IO](
    "scala-sdk-test",
    "extractor-bluefield-testing",
    "https://bluefield.cognitedata.com",
    authProvider,
    None,
    None,
    None
  )

  private def getNonce: String = client.sessions
    .createWithClientCredentialFlow(Items(Seq(SessionCreateWithCredential(credentials.clientId, credentials.clientSecret)))).unsafeRunSync()
    .map(_.nonce).head

  //Further test assume this function already exists
  //They also assume the function with this ID has some completed calls already
  private val preExistingFunctionId: Long = 8590831424885479L

  //Retrieve a completed call to be used in tests
  //Due to 90 days retention policy, this needs to be dynamic
  //This assumes the function "preExistingFunctionId" has existing Completed calls
  private lazy val preExistingCallId: Long = {
    val filter = FunctionCallFilter(status = Some("Completed"))
    val filtered = client.functionCalls(preExistingFunctionId).filter(filter).unsafeRunSync()
    filtered.items shouldNot be(empty)
    filtered.items.head.status should equal("Completed")
    filtered.items.head.id
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
    val call = client.functionCalls(preExistingFunctionId).retrieveById(preExistingCallId).unsafeRunSync()
    call.id should equal(preExistingCallId)
  }

  it should "read function call logs items" in {
    val res = client.functionCalls(preExistingFunctionId).retrieveLogs(preExistingCallId).unsafeRunSync()
    res.items.isEmpty shouldBe true
  }

  it should "retrieve function call response" in {
    val response = client.functionCalls(preExistingFunctionId).retrieveResponse(preExistingCallId).unsafeRunSync()
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
