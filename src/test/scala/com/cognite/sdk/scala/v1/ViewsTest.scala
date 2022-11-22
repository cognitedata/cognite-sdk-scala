// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1

import cats.effect.unsafe.implicits.global
import com.cognite.sdk.scala.common.{DSLAndFilter, DSLEqualsFilter, DSLInFilter, RetryWhile, SdkException}
import com.cognite.sdk.scala.v1.containers.ContainerReference

import java.util.UUID

@SuppressWarnings(
  Array(
    "org.wartremover.warts.PublicInference",
    "org.wartremover.warts.NonUnitStatements"
  )
)
class ViewsTest extends CommonDataModelTestHelper with RetryWhile {
  it should "create views" in {
    val uuid = UUID.randomUUID.toString // TODO no need to use uuid for externalId when API is in place
    val implements = Seq(ViewReference(space = "space", externalId = "viewExternalId", version = Some("1.0.1")))
    val containerReference = ContainerReference("space", "containerExternalId")
    val properties = Map("prop1" -> CreatePropertyReference(container = containerReference,
      externalId = "cont1", name = Some("cont1"), description = Some("hello"))
    )
    val created: ViewDefinition = localClient.views.createItems(Seq(
      ViewCreateDefinition(
         space = "test",
         externalId = uuid,
         name = Some("test"),
         description = Some("desc"),
         filter = Some(DSLAndFilter(Seq(
           DSLInFilter(property = Seq("dummy", "dummy2"), `values` = Seq(
             PropertyType.Bigint.Property(9223372036854775L),
             PropertyType.Text.Property("abcdef"),
           )),
           DSLEqualsFilter(property = Seq("dummy"), `value` = PropertyType.Text.Property("testValue"))))),
         implements = Some(implements),
         version = Some("5.0.0"),
         properties = properties
    ))).unsafeRunSync()
    created.space shouldBe "test"
    created.externalId shouldBe uuid
    created.name shouldBe Some("test")
    created.description shouldBe Some("desc")
    created.implements shouldBe Some(implements)
    created.version shouldBe Some("5.0.0")

    created.properties shouldBe  Map("prop1" -> ViewPropertyDefinition(externalId = "cont1",
      nullable = None,
      autoIncrement = None,
      description = Some("hello"),
      `type` = None,
      container = Some(containerReference),
      containerPropertyExternalId = None)
    )
  }

  it should "retrieve views by data model reference" in {
    val uuid = UUID.randomUUID.toString // TODO no need to use uuid for externalId when API is in place

    localClient.views.createItems(Seq(
      ViewCreateDefinition(
        space = "test1",
        externalId = uuid,
        name = Some("test1"),
        description = Some("desc"),
        filter = None,
        implements = None,
        version = Some("6.0.0"),
        properties = Map()
      ))).unsafeRunSync()

    val retrieved = localClient.views.retrieveItems(Seq(DataModelReference("test1", uuid, "6.0.0"))).unsafeRunSync().head
    retrieved.space shouldBe "test1"
    retrieved.externalId shouldBe uuid
    retrieved.name shouldBe Some("test1")
    retrieved.description shouldBe Some("desc")
    retrieved.version shouldBe Some("6.0.0")
  }

  it should "delete views" in {
    val uuid = UUID.randomUUID.toString // TODO no need to use uuid for externalId when API is in place

    localClient.views.createItems(Seq(
      ViewCreateDefinition(
        space = "test",
        externalId = uuid,
        name = Some("test"),
        description = Some("desc"),
        filter = None,
        implements = None,
        version = Some("5.0.0"),
        properties = Map()
      ))).unsafeRunSync()
    val retrieved = localClient.views.retrieveItems(Seq(DataModelReference("test", uuid, "5.0.0"))).unsafeRunSync()
    retrieved.head.externalId shouldBe uuid

    localClient.views.deleteItems(Seq(DataModelReference("test", uuid, "5.0.0"))).unsafeRunSync()
    val retrievedAfterDelete = localClient.views.retrieveItems(Seq(DataModelReference("test", uuid, "5.0.0"))).unsafeRunSync()
    retrievedAfterDelete.size shouldBe(0)

    // TODO This should produce CdpAPIException
    val sCaught = intercept[SdkException] {
      localClient.views.deleteItems(Seq(DataModelReference("test", "test", "test"))).unsafeRunSync()
    }
    sCaught.responseCode shouldBe  Some(404)
  }
}
