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

  it should "read function items" in {
    client.functions.read().unsafeRunSync().items should not be empty
  }

  it should "retrieve function by id" in {
    val func = client.functions.retrieveById(8590831424885479L).unsafeRunSync()
    func.id should equal(Some(8590831424885479L))
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
    client.functionCalls(8590831424885479L).read().unsafeRunSync().items should not be empty
  }

  it should "retrieve function call by id" in {
    val call = client.functionCalls(8590831424885479L).retrieveById(3987350585370826L).unsafeRunSync()
    call.id should equal(3987350585370826L)
    call.status should equal("Completed")
  }

  it should "read function call logs items" in {
    val res = client.functionCalls(8590831424885479L).retrieveLogs(3987350585370826L).unsafeRunSync()
    res.items.isEmpty shouldBe true
  }

  it should "retrieve function call response" in {
    val response = client.functionCalls(8590831424885479L).retrieveResponse(3987350585370826L).unsafeRunSync()
    response.response shouldBe Json.fromFields(Seq(("res_int", Json.fromInt(1)),("res_string", Json.fromString("string response"))))
  }

  it should "filter function calls" in {
    val filter = FunctionCallFilter(status = Some("Completed"))
    val res = client.functionCalls(8590831424885479L).filter(filter).unsafeRunSync().items
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
    val nonce = client.sessions
      .createWithClientCredentialFlow(Items(Seq(SessionCreateWithCredential(credentials.clientId, credentials.clientSecret)))).unsafeRunSync()
      .map(_.nonce).head
    val res = client.functionCalls(8590831424885479L).callFunction(Json.fromJsonObject(JsonObject.empty), Some(nonce)).unsafeRunSync()
    res.status should equal("Running")
  }

  it should "create and delete function schedules" in {
    val nonce = client.sessions
      .createWithClientCredentialFlow(Items(Seq(SessionCreateWithCredential(credentials.clientId, credentials.clientSecret)))).unsafeRunSync()
      .map(_.nonce).head
    val createRes = client.functionSchedules.create(Seq(
      FunctionScheduleCreate(
        name = "scala-sdk-write-example-1",
        functionId = Some(8590831424885479L),
        cronExpression = "0 0 1 * *",
        nonce = Some(nonce)
      )
    )).unsafeRunSync()
    createRes.length should equal(1)
    client.functionSchedules.deleteByIds(createRes.map(_.id.getOrElse(0L))).unsafeRunSync()
    client.functionSchedules.read().unsafeRunSync().items.find(_.name === "scala-sdk-write-example-1") shouldBe None
  }
}
